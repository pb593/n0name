package core; /**
 * Created by pb593 on 19/11/2015.
 */

import exception.MessengerOfflineException;
import exception.UserIDTakenException;
import gui.ClientGUI;
import message.InviteMessage;
import message.Message;
import message.TextMessage;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.parser.ParseException;
import scaffolding.AddressBook;
import scaffolding.Utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Client implements Runnable {

    private final String userID;
    private final Communicator comm;
    private final ConcurrentHashMap<String, Clique> cliques = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> addressTags = new ConcurrentHashMap<>();
                                                            // for fast lookup addressTag -> cliqueName
    private final ClientGUI gui; // GUI instance


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
        gui = new ClientGUI(this);
    }

    public String getUserID() {
        return this.userID;
    }

    public List<String> getGroupList() {
        return new ArrayList<>(cliques.keySet());
    }

    public List<String> getOnlineUsers() {
        Set<String> users = null;
        try {
            users = AddressBook.getAll().keySet();
        } catch (MessengerOfflineException e) { } // if we are online


        return users != null ? new ArrayList<>(users) : new ArrayList<>();
    }

    public List<String> getGroupParticipants(String cliqueName) {
        if (cliques.containsKey(cliqueName))
            return cliques.get(cliqueName).getUserList();
        else
            return new ArrayList<>();
    }

    public List<TextMessage> getMessageHistory(String cliqueName) {
        if(cliques.containsKey(cliqueName))
            return cliques.get(cliqueName).getHistory();
        else
            return new ArrayList<>(); // return empty list
    }

    public boolean sendMessage(String cliqueName, String txt) {
        if(cliques.containsKey(cliqueName)) {
            cliques.get(cliqueName).sendMessage(new TextMessage(txt, this.userID, cliqueName));
            return true;
        }
        else
            return false;

        // if do not have a clique like this – just drop it
    }

    public boolean addParticipant(String cliqueName, String newUserID) { // return true if ok an false otherwise
        if(cliques.containsKey(cliqueName)) {
            cliques.get(cliqueName).addMember(newUserID);
            return true;
        }
        else
            return false;

    }

    public boolean createGroup(String newCliqueName) { // return true if group created and false if already exists
        if(!cliques.containsKey(newCliqueName)) { // no such group exists yet
            Clique c = new Clique(newCliqueName, this, comm);
            cliques.put(newCliqueName, c); // insert new clique
            addressTags.put(c.getCurrentAddressTag(), newCliqueName);
            c.start(); // patching and sealing component
            return true;
        }
        else // group already exists
            return false;
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
                gui.updateContent(); // force GUI to display the new group
            }
            else { // InviteResponseMessage
                cliques.get(msg.cliqueName).messageReceived(msg); // pass the message to clique
                gui.updateContent(); // force GUI reflect the changes
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
                    gui.updateContent(); // force GUI reflect the changes
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

    @Override
    public void run() {
        // main function of Client

        // start separate thread to report my address to address server
        // also, update GUI on whether we are online/offline
        Thread th = new Thread() {
            @Override
            public void run() {
                while(true) { // external loop – for error handling
                    try {
                        AddressBook.init(); // initialize the address book

                        while (true) { // check into the book every 5 sec
                            AddressBook.checkin(userID, comm.getPort());
                            if(gui != null) gui.setIsOnline(true); // tell gui we are online

                            Utils.sleep(5000);
                        }
                    } catch (MessengerOfflineException e) { // messenger is offline
                        if(gui != null) gui.setIsOnline(false); // tell GUI we are offline
                        Utils.sleep(10000); // wait for a bit longer, then try to reconnect
                    }
                }

            }
        };
        th.setDaemon(true);
        th.start();

        comm.start(); //start our communicator

        gui.setVisible(true); // run GUI
        //cli(); // run the CLI


    }

    /* CLI funcs down here */

    private void cli() {
        // CLI starts here
        Scanner scanner = new Scanner(System.in);
        while(true) {
            cls(); // clear screen
            System.out.println("NoNaMe Chat\u2122");

            // output other members which are currently online
            HashMap<String, InetSocketAddress> users = null;
            try {
                users = AddressBook.getAll(); // this could throw MessengerOfflineException
                if(users.size() > 0) { // more than one person (me) online
                    System.out.println("Users online:");
                    for(String otherUser: users.keySet()) {
                        System.out.printf(" - %s\n", otherUser);
                    }
                }
                else {
                    System.out.println("None of the other NoNaMe users are currently online.");
                }

                // output which groups I'm currently in
                if(cliques.keySet().size() > 0) { // if there are any active cliques
                    System.out.println("Groups:");
                    for (String cliqueName : cliques.keySet()) {
                        System.out.printf(" - %s\n", cliqueName);
                    }
                }
                else { // if there are no active cliques
                    System.out.println("You are not part of any group at the moment.");
                }
                System.out.printf("> "); // invitation to type in a command
            } catch (MessengerOfflineException e) {
                System.out.println("The messenger is offline at the moment :-(");
            }

            String str = scanner.nextLine();
            if(str.equals("")) { // REFRESH
                continue;
            }
            else if(str.equals("exit")) // EXIT program
                break;
            else if (str.equals("help")) {
                help();
            }
            else if(str.startsWith("view")) { // VIEW command
                String[] tokens = str.split("\\s+"); //split command on whitespace
                if(tokens.length >= 2) {
                    String cliqueName = tokens[1];
                    if(cliques.containsKey(cliqueName)) {
                        view(cliques.get(cliqueName));
                    }
                    else {
                        System.out.printf("Group named '%s' not found...\n", cliqueName);
                        pressAnyKeyToContinue();
                    }
                }
                else {
                    System.out.println("FORMAT: view <groupName>");
                    pressAnyKeyToContinue();
                }
            }
            else if(str.startsWith("create")) { // CREATE command
                String[] tokens = str.split("\\s+"); //split command on whitespace
                if(tokens.length >= 2) {
                    String newCliqueName = tokens[1];
                    if(!cliques.containsKey(newCliqueName)) { // if name is fresh
                        Clique c = new Clique(newCliqueName, this, comm);
                        cliques.put(newCliqueName, c); // insert new clique
                        addressTags.put(c.getCurrentAddressTag(), newCliqueName);
                        c.start(); // patching and sealing component
                        System.out.printf("A new empty group with name '%s' has been created.\n", newCliqueName);
                    }
                    else {
                        System.out.printf("Group with name '%s' already exists.\n", newCliqueName);
                        pressAnyKeyToContinue();
                    }
                }
                else {
                    System.out.println("FORMAT: create <newGroupName>");
                    pressAnyKeyToContinue();
                }
            }
            else if(str.startsWith("add")) { // ADD command
                String[] tokens = str.split("\\s+"); //split command on whitespace
                if(tokens.length >= 3) {
                    String userID = tokens[1];
                    String groupName = tokens[2];
                    try {
                        if(cliques.containsKey(groupName) && AddressBook.contains(userID)){
                            Clique c = cliques.get(groupName); // get clique with this name
                            c.addMember(userID); // add user to group
                            System.out.printf("Successfully added user '%s' to group '%s'\n", userID, groupName);
                        }
                        else {
                            System.out.printf("Group with name '%s' or user with name '%s' does not exist\n", groupName,
                                    userID);
                        }
                    } catch (MessengerOfflineException e) {
                        continue; // just go to the beginning
                    }
                }
                else {
                    System.out.println("FORMAT: add <userID> <groupName>");
                    pressAnyKeyToContinue();
                }
            }
            else {
                System.out.println("Malformed command. Try again...");
                pressAnyKeyToContinue();
            }
        }
    }

    private void help() {
        cls();
        System.out.println("TODO");
        pressAnyKeyToContinue();
    }

    private void pressAnyKeyToContinue() {
        System.out.println("Press any key to continue...");
        try
        {
            System.in.read();
        }
        catch(Exception e)
        {}
    }

    private void cls() {
        try {
            Runtime.getRuntime().exec("clear");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void view(Clique clique) {
        Scanner scanner = new Scanner(System.in);
        while(true) {
            cls();
            System.out.printf("Group Name: %s\n", clique.getCliqueName());
            System.out.printf("Members: %s\n", StringUtils.join(clique.getUserList(), ", "));
            List<TextMessage> last5 = clique.getLastFive();
            if(last5.size() > 0) {
                System.out.printf("Last few messages:\n");
                for(TextMessage msg: last5) {
                    System.out.printf("\t%s: %s\n", msg.author, msg.text);
                }
            }
            else {
                System.out.println("There are no messages in this group... yet.");
            }

            System.out.printf("> "); // invitation to type in a command
            String str = scanner.nextLine();
            if(str.equals("")) // REFRESH
                continue;
            else if(str.equals("exit")) // EXIT
                System.exit(0); //terminate the chat client
            else if(str.equals("back")) // back to main menu
                return;
            else if(str.startsWith("msg")) { // WRITE MESSAGE
                String txt = (str.charAt(3) == ' ') ? str.substring(4) : str.substring(3);
                TextMessage newmsg = new TextMessage(txt, this.userID, clique.getCliqueName()); // create new message
                clique.sendMessage(newmsg); //send message to clique using Communicator
                System.out.println("Message sent.");
                pressAnyKeyToContinue();
            }
            else if(str.startsWith("add")) { // add new user to currently viewed group
                String[] tokens = str.split("\\s+"); //split command on whitespace
                if(tokens.length >= 2) {
                    String userID = tokens[1];
                    try {
                        if(cliques.containsKey(clique.getCliqueName()) && AddressBook.contains(userID)){
                            Clique c = cliques.get(clique.getCliqueName()); // get clique with this name
                            c.addMember(userID); // add user to group
                            System.out.printf("Successfully added user '%s' to group '%s'\n", userID, clique.getCliqueName());
                        }
                        else {
                            System.out.printf("Group with name '%s' or user with name '%s' does not exist\n",
                                    clique.getCliqueName(), userID);
                        }
                    } catch (MessengerOfflineException e) {
                        return; // go to main menu
                    }
                }
                else {
                    System.out.println("FORMAT: add <userID>");
                    pressAnyKeyToContinue();
                }
            }
            else {
                System.out.println("Malformed command. Try again...");
                pressAnyKeyToContinue();
            }
        }
    }


}
