package ui;

import core.Client;
import core.Clique;
import exception.MessengerOfflineException;
import exception.UserIDTakenException;
import message.TextMessage;
import org.apache.commons.lang3.StringUtils;
import scaffolding.AddressBook;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/**
 * Created by pb593 on 29/03/2016.
 */
public class CLIClient extends Client {

    private boolean isOnline = true;
    private final Object isOnlineLock = new Object(); // dummy object to sync on when accessing isOnline

    public CLIClient(String userID) throws UserIDTakenException, MessengerOfflineException {
        super(userID);
    }

    @Override
    public void run() {
        super.run();
        cli();
    }

    /* CLI funcs down here */

    private void cli() {
        // CLI starts here
        Scanner scanner = new Scanner(System.in);

        while(true) {
            cls(); // clear screen
            synchronized (isOnlineLock) {
                System.out.println("NoNaMe Chat\u2122 " + (this.isOnline ? "" : "[offline]"));
            }

            // output other members which are currently online
            HashMap<String, InetSocketAddress> users = null;
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
                    if(cliques.containsKey(groupName) && AddressBook.contains(userID)){
                        Clique c = cliques.get(groupName); // get clique with this name
                        boolean success = c.addMember(userID); // add user to group
                        if(success)
                            System.out.printf("Successfully added user '%s' to group '%s'\n", userID, groupName);
                        else
                            System.err.printf("Unable to add user '%s' to group." +
                                    "Unable to reach them with an invitation message.", userID);
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
            synchronized (isOnlineLock) {
                System.out.printf("Group Name: %s%s\n ", clique.getCliqueName(), isOnline ? "" : " [offline]");
            }
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
                    if(cliques.containsKey(clique.getCliqueName()) && AddressBook.contains(userID)){
                        Clique c = cliques.get(clique.getCliqueName()); // get clique with this name
                        boolean success = c.addMember(userID); // add user to group
                        if(success)
                            System.out.printf("Successfully added user '%s' to group '%s'\n",
                                                                                userID, clique.getCliqueName());
                        else
                            System.err.printf("Unable to add user '%s' to group." +
                                    "Unable to reach them with an invitation message.", userID);
                    }
                    else {
                        System.out.printf("Group with name '%s' or user with name '%s' does not exist\n",
                                clique.getCliqueName(), userID);
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

    @Override
    protected void updateContent() {
        // CLI is not real-time, so all updates are displayed when user hits 'RETURN'
        return; // doing nothing
    }

    @Override
    protected void setIsOnline(boolean isOnline) {
        // CLI is not real-time, so it just gives notification to user about offline status when they hit 'RETRUN'
        synchronized (isOnlineLock) {
            this.isOnline = isOnline; // update status
        }
    }

}
