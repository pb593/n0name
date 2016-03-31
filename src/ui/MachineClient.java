package ui;

import core.Client;
import core.Clique;
import exception.MessengerOfflineException;
import exception.UserIDTakenException;
import message.TextMessage;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import scaffolding.AddressBook;

import java.util.List;
import java.util.Scanner;

/**
 * This is a simplified version of command-line interface, designed for interaction with a testing script.
 * It has the following commands (request, response, function):
 *
 *  groups,                         <space-separated list of groups>,   returns list of groups user is in
 *  peers,                          <list of peers currently online>,   returns all users in address book (space-separated)
 *  isOnline,                       online/offline,                     yes iff chat is online
 *  create <groupID>,               ACK/Error,                          request to create a group
 *  add <userID> <groupID>,         ACK/Error,                          request to add user to a group
 *  msg <groupID> <"message text">, ACK/Error,                          request to send message to a group
 *  history <groupID>,              JSON([(author, text), ...]),        returns list of messages in JSON in time order
 *  members <groupID>,              <space separated list of users>,    returns list of users in a given group
 *  exit,                           ACK, *shuts down*,                  close the program instance
 *
 *
 * There is strictly 1 whitespace in the separators.
 */
public class MachineClient extends Client {

    public static final Integer interfaceCode = 2;

    private boolean isOnline = true;
    private final Object isOnlineLock = new Object(); // dummy object to sync on when accessing isOnline

    public MachineClient(String userID) throws UserIDTakenException, MessengerOfflineException {
        super(userID);
    }

    @Override
    public void run() {
        super.run();
        machineInterface();
    }

    private void machineInterface() {
        Scanner scanner = new Scanner(System.in);

        while(true) {
            String cmd = scanner.nextLine().trim(); // read command
            if(cmd.equals("groups")) {
                System.out.println(StringUtils.join(cliques.keySet(), " "));
            }
            else if(cmd.equals("status")) {
                synchronized (isOnlineLock) {
                    System.out.println(isOnline ? "online" : "offline");
                }
            }
            else if(cmd.equals("exit")) {
                System.out.println("ACK");
                break;
            }
            else if(cmd.equals("peers")) {
                System.out.println(StringUtils.join(AddressBook.getAll().keySet(), " "));
            }
            else {
                String[] tokens = cmd.split("\\s"); // split on SINGLE WHITESPACE
                if(tokens[0].equals("create") && tokens.length >= 2) {
                    String newCliqueName = tokens[1];
                    if(!cliques.containsKey(newCliqueName)) { // if name is fresh
                        Clique c = new Clique(newCliqueName, this, comm);
                        cliques.put(newCliqueName, c); // insert new clique
                        addressTags.put(c.getCurrentAddressTag(), newCliqueName);
                        c.start(); // patching and sealing component
                        System.out.println("ACK");
                    }
                    else
                        System.out.println("Error:AlreadyExists");
                }
                else if(tokens[0].equals("history") && tokens.length >=2) {
                    String groupID = tokens[1];
                    if(cliques.containsKey(groupID)) {
                        JSONArray array = new JSONArray();
                        List<TextMessage> history = cliques.get(groupID).getHistory();
                        for(TextMessage tm: history) { // chronological order
                            JSONObject jsonMsg = new JSONObject();
                            jsonMsg.put("author", tm.author);
                            jsonMsg.put("text", tm.text);
                            array.add(jsonMsg);
                        }
                        System.out.println(array.toJSONString()); // output history in JSON format
                    }
                    else {
                        System.out.println("Error:UnknownGroup");
                    }
                }
                else if(tokens[0].equals("members") && tokens.length >=2) {
                    String groupID = tokens[1];
                    if(cliques.containsKey(groupID)) // if group is known
                        System.out.println(StringUtils.join(cliques.get(groupID).getUserList(), " "));
                    else
                        System.out.println("Error:UnknownGroup");

                }
                else if(tokens[0].equals("add") && tokens.length >= 3) {
                    String userID = tokens[1];
                    String groupName = tokens[2];
                    if(cliques.containsKey(groupName) && AddressBook.contains(userID) &&
                                                                                !userID.equals(myself.getUserID())){
                        Clique c = cliques.get(groupName); // get clique with this name
                        boolean success = c.addMember(userID); // add user to group
                        if(success)
                            System.out.println("ACK");
                        else
                            System.err.printf("Error:FailedToSendInvitation");
                    }
                    else {
                        System.out.printf("Error:InvalidArguments");
                    }
                }
                else if (tokens[0].equals("msg") && tokens.length >=3) {
                    String groupID = tokens[1];
                    if(cliques.containsKey(groupID)) {
                        Clique clique = cliques.get(groupID);
                        String txt = cmd.substring(3 + 1 + groupID.length() + 1);
                        TextMessage newmsg = new TextMessage(txt, this.userID, groupID); // create new message
                        clique.sendMessage(newmsg); //send message to clique using Communicator
                        System.out.println("ACK");
                    }
                    else
                        System.out.println("Error:UnknownGroup");
                }
                else {
                    System.out.println("InvalidCommand");
                    continue;
                }
            }

        }
    }

    @Override
    protected void updateContent() {
        // machine interface does not need this function
        return; // doing nothing
    }

    @Override
    protected void setIsOnline(boolean isOnline) {
        synchronized (isOnlineLock) {
            this.isOnline = isOnline; // update status
        }
    }
}
