package core; /**
 * Created by pb593 on 19/11/2015.
 */

import com.sun.deploy.util.StringUtils;
import exception.UserIDTakenException;
import message.InviteMessage;
import message.Message;
import message.TextMessage;
import org.json.simple.parser.ParseException;
import scaffolding.AddressBook;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Client implements Runnable {

    private final String userID;
    private final Communicator comm;
    private final ConcurrentHashMap<String, Clique> cliques = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> addressTags = new ConcurrentHashMap<>();
                                                            // for fast lookup addressTag -> cliqueName

    public Client(String userID) throws UserIDTakenException {

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
        return this.userID;
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
                cliques.put(msg.cliqueName, c); // put into the clique hashmap
            }
            else {
                cliques.get(msg.cliqueName).messageReceived(msg); // pass the message to clique
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
                } else { // never see this clique before and isn't an invitation
                    System.err.printf("Received message for non-existent clique '%s'. Dropping it.\n", cliqueName);
                }
            }
            else
                System.err.printf("Received a message for unknown clique with address tag: %s\n", tag);
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
        comm.start(); //start our communicator

        // start separate thread to report my address to address server
        Thread th = new Thread() {
            @Override
            public void run() {

                while(true) {
                    AddressBook.checkin(userID, comm.getPort());
                    try {
                        Thread.sleep(5000); // sleep for 5 seconds
                    } catch (InterruptedException e) {
                        System.err.print("Sleep() call failed in address reporting thread.");
                    }
                }

            }
        };
        th.setDaemon(true);
        th.start();

        // CLI starts here
        Scanner scanner = new Scanner(System.in);
        while(true) {
            cls(); // clear screen
            System.out.println("NoNaMe Chat\u2122");

            // output other members which are currently online
            HashMap<String, InetSocketAddress> users = AddressBook.getAll();
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
                    if(cliques.containsKey(groupName) && AddressBook.contains(userID)){
                        Clique c = cliques.get(groupName); // get clique with this name
                        c.addMember(userID); // add user to group
                        System.out.printf("Successfully added user '%s' to group '%s'\n", userID, groupName);
                    }
                    else {
                        System.out.printf("Group with name '%s' or user with name '%s' does not exist\n", groupName,
                                userID);
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

    /* CLI funcs down here */

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
            System.out.printf("Group Name: %s\n", clique.getName());
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
                TextMessage newmsg = new TextMessage(txt, this.userID, clique.getName()); // create new message
                clique.sendMessage(newmsg); //send message to clique using Communicator
                System.out.println("Message sent.");
                pressAnyKeyToContinue();
            }
            else if(str.startsWith("add")) { // add new user to currently viewed group
                String[] tokens = str.split("\\s+"); //split command on whitespace
                if(tokens.length >= 2) {
                    String userID = tokens[1];
                    if(cliques.containsKey(clique.getName()) && AddressBook.contains(userID)){
                        Clique c = cliques.get(clique.getName()); // get clique with this name
                        c.addMember(userID); // add user to group
                        System.out.printf("Successfully added user '%s' to group '%s'\n", userID, clique.getName());
                    }
                    else {
                        System.out.printf("Group with name '%s' or user with name '%s' does not exist\n",
                                                                                            clique.getName(), userID);
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
