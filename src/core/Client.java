package core; /**
 * Created by pb593 on 19/11/2015.
 */

import exception.MessengerOfflineException;
import exception.UserIDTakenException;
import message.InviteMessage;
import message.Message;
import org.json.simple.parser.ParseException;
import scaffolding.AddressBook;
import scaffolding.Utils;
import ui.CLIClient;
import ui.GUIClient;
import ui.MachineClient;

import javax.swing.*;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

abstract public class Client implements Runnable {


    protected final Client myself = this; // for nestedly included classes to use
    protected final String userID;
    protected final ConcurrentHashMap<String, Clique> cliques = new ConcurrentHashMap<>();
    protected final Communicator comm;
    protected final ConcurrentHashMap<String, String> addressTags = new ConcurrentHashMap<>();
                                                                // for fast lookup addressTag -> cliqueName

    // abstract methods for all implementations to define
    abstract protected void updateContent();
    abstract protected void setIsOnline(boolean isOnline);

    public static void main(String[] argv) {
        // main function of the client

        // first, whether user wants a GUI, CLI or machine interface
        int interfaceCode = GUIClient.interfaceCode; // GUI is default interface

        if(argv.length >= 1) { // we have at least one option -> interface has been specified
            if(argv[0].equals("-cli"))
                interfaceCode = CLIClient.interfaceCode;
            else if(argv[0].equals("-m"))
                interfaceCode = MachineClient.interfaceCode;
            else // some sort of other option specified
                interfaceCode = GUIClient.interfaceCode; // fall back to GUI
        }

        if(argv.length >= 2) { // we have 2 or more options ->  patching period has been specified
            double PATCH_PERIOD = Double.parseDouble(argv[1]);
            Utils.PATCH_FREQ = 1.0 / PATCH_PERIOD;
        }

        // try to reach out to address book
        try {
            AddressBook.init(); // initialize the address book
        } catch (MessengerOfflineException e) { // if we are offline
            if(interfaceCode == CLIClient.interfaceCode) { // CLI mode
                System.out.println("You seem to be offline. Check you network connection and restart application");
            }
            else if(interfaceCode == GUIClient.interfaceCode) { // GUI mode
                JOptionPane.showMessageDialog(null,
                        "You seem to be offline. Check you network connection and restart application", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
            else {} // machine interface just silently exits with error
            System.exit(1);
        }

        Client cl = null;
        Scanner scanner = new Scanner(System.in);
        while(true) {
            String userID = null;
            String rndUserID = Utils.randomAlphaNumeric(10);
            if(interfaceCode == CLIClient.interfaceCode) { // if we are in CLI mode
                System.out.printf("Please choose your username (suggestion: %s):", rndUserID);
                userID = scanner.nextLine();
            }
            else if(interfaceCode == GUIClient.interfaceCode) { // if we are in GUI mode
                userID = (String) JOptionPane.showInputDialog(
                        null,
                        "Please pick a username",
                        "Pick username",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        rndUserID);

                if(userID == null) // 'Cancel' button pressed
                    System.exit(0); // just exit
            }
            else { // machine interface
                if(argv.length >= 3) userID = argv[2]; else System.exit(1); // takes user name as shell argument
            }

            try {
                if(userID.split("\\s+").length != 1) {// if there were spaces in the userID
                    String error_msg = "The username you entered contains whitespace. This is not allowed. Try again.";
                    if(interfaceCode == CLIClient.interfaceCode) { // CLI mode
                        System.out.println(error_msg);
                    }
                    else if(interfaceCode == GUIClient.interfaceCode) { // GUI mode
                        JOptionPane.showMessageDialog(null, error_msg, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    else { // machine interface
                        System.out.println("Error:WhiteSpaceInUsername");
                        System.exit(1);
                    }
                }
                else { // username is valid
                    if(interfaceCode == CLIClient.interfaceCode) // CLI mode
                        cl = new CLIClient(userID);
                    else if(interfaceCode == GUIClient.interfaceCode)// GUI mode
                        cl = new GUIClient(userID);
                    else // machine interface
                        cl = new MachineClient(userID);

                    break; // exit loop if Client successfully constructed
                }
            } catch (UserIDTakenException e) { // if this user name taken
                String error_msg = "This user name is taken. Please try another one.";
                if(interfaceCode == CLIClient.interfaceCode) { // CLI mode
                    System.out.println(error_msg);
                }
                else if(interfaceCode == GUIClient.interfaceCode) { // GUI mode
                    JOptionPane.showMessageDialog(null, error_msg, "Error", JOptionPane.ERROR_MESSAGE);
                }
                else { // machine interface
                    System.out.println("Error:UserNameTaken");
                    System.exit(1);
                }
            } catch (MessengerOfflineException e) { // we are offline
                String error_msg = "You appear to be offline. Connect to network and try again.";
                if(interfaceCode == CLIClient.interfaceCode) { // CLI mode
                    System.out.println("You appear to be offline. Connect to network and try again.");
                }
                else if(interfaceCode == GUIClient.interfaceCode) { // GUI mode
                    JOptionPane.showMessageDialog(null, error_msg, "Error", JOptionPane.ERROR_MESSAGE);
                }
                else { // machine interface
                    System.out.println("Error:WeAreOffline");
                    System.exit(1);
                }
            }
        }

        cl.run(); // run client in the same thread (dynamic polymorphism should work here)

    }

    @Override
    public void run() {
        // start separate thread to report my address to address server
        // also, update GUI on whether we are online/offline
        Thread th = new Thread() {
            @Override
            public void run() {
                while(true) { // external loop – for error handling
                    try {
                        while (true) { // check into the book every 5 sec
                            AddressBook.checkin(userID, comm.getPort());
                            myself.setIsOnline(true); // tell gui we are online

                            Utils.sleep(5000);
                        }
                    } catch (MessengerOfflineException e) { // messenger is offline
                        myself.setIsOnline(false); // tell GUI we are offline
                        Utils.sleep(10000); // wait for a bit longer, then try to reconnect
                    }
                }

            }
        };
        th.setDaemon(true);
        th.start();

        comm.start(); //start our communicator
    }


    public Client(String userID) throws UserIDTakenException, MessengerOfflineException {

        if(AddressBook.contains(userID)) // userID is already in use
            throw new UserIDTakenException();

        Random rnd = new Random();
        int port = 0; // prepare to choose randomly
        Communicator commtmp = null;
        while(true) { //look for a free port
            port = 50000 + rnd.nextInt(10000); //choose a random port between 50k and 60k
            try {
                commtmp = new Communicator(this, port); //try to bind to port
                // start communicator, giving it a reference back to Client
            } catch (IOException e) { // can't bind to the port
                System.err.print("Communicator unable to bind to port " + port + ". Looking for another one.");
                continue;
            }
            break;
        }
        this.userID = userID;
        comm = commtmp;
    }

    public String getUserID() {
        return userID;
    }


    public void msgReceived(String datagramStr) {
        /* Callback received in a dedicated thread from Communicator.
        *  Function: demultiplex message into the right clique. */

        if(datagramStr.startsWith("NoNaMe")) { // unencrypted communication -> can recover Message here
            datagramStr = datagramStr.replaceFirst("NoNaMe", ""); // remove decryption marker
            Message msg = null;
            try {
                msg = Message.fromJSON(datagramStr); // recover the message object
            } catch (ParseException e) {
                System.err.printf("Unable to parse and incoming message. Dropping it.");
                return; // just ignore the message
            }
            if(msg instanceof InviteMessage) { // if somebody added me to this clique
                Clique c = new Clique(msg.cliqueName, this, comm, (InviteMessage) msg); // cliques are never empty, so do not need checks
                cliques.put(msg.cliqueName, c); // put into the clique hash map
                c.start(); // patching and sealing component
                this.updateContent(); // force GUI to display the new group
            }
            else { // InviteResponseMessage
                cliques.get(msg.cliqueName).messageReceived(msg); // pass the message to clique
                this.updateContent(); // force GUI reflect the changes
            }
        }
        else { // encrypted communication
            String tag = datagramStr.substring(0, Cryptographer.macB64StringLength);
            if(addressTags.containsKey(tag)) {
                String cliqueName = addressTags.get(tag);
                datagramStr = datagramStr.substring(Cryptographer.macB64StringLength); // remove the address tag from message
                if (cliques.containsKey(cliqueName)) { // if clique is already known to me
                    Clique c = cliques.get(cliqueName);
                    c.datagramReceived(datagramStr); // give callback to the specific clique
                    this.updateContent(); // force GUI reflect the changes
                } else { // never see this clique before and isn't an invitation
                    System.err.printf("Received message for non-existent clique '%s'. Dropping it.\n", cliqueName);
                }
            }
            else // Received a message clique with unknown address tag
                return; // just drop it
        }

    }

    public void addAddressTag(String newAddressTag, String cliqueName) {
        /* Clique publishes its address tag in Client to receive messages destined for it*/
        synchronized (addressTags) {
            addressTags.put(newAddressTag, cliqueName);
        }
    }

    public void removeAddressTag(String oldAddressTag) {
        /* When address tag changes, Clique removes it */
        synchronized (addressTags) {
            addressTags.remove(oldAddressTag);
        }
    }


}
