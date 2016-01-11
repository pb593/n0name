import message.*;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by pb593 on 19/11/2015.
 */

public class Clique {

    private final String name;
    private final HashMap<String, User> members = new HashMap<>();
    private final List<TextMessage> messageHistory = new ArrayList<>(); // messages in the order of arrival
    private final Client client;
    private final Communicator comm;
    private final HashSet<String> pendingInvites = new HashSet<>(); // users I invited and waiting for response

    public Clique(String name, Client client, Communicator comm) {
        this.name = name;
        this.client = client;
        this.comm = comm;

        // put myself in the Clique
        synchronized (members) {
            members.put(client.getUserID(), new User(client.getUserID()));
        }
    }

    public Clique(String name, Client client, Communicator comm, List<String> listOfUsers) {
        // this constructor is used if a third party adds me to the clique

        this.name = name;
        this.client = client;
        this.comm = comm;

        // put all users into the members list (including myself)
        synchronized (members) {
            members.put(client.getUserID(), new User(client.getUserID()));
            for (String username : listOfUsers) {
                members.put(username, new User(username));
            }
        }
    }

    public void addMember(String userID) {

        if(AddressBook.contains(userID)) {
            // get address of the destination
            InetSocketAddress dest = AddressBook.lookup(userID);

            // send the invite
            synchronized (members) {
                comm.send(dest, new InviteMessage(members.keySet(), client.getUserID(), this.name));
            }

            // mark invite as pending response
            pendingInvites.add(userID);
        }
        else {
            System.err.printf("Unable to add user '%s' to group. User is not in address book.\n", userID);
        }
    }

    public String getName() {
        return name;
    }

    public List<TextMessage> getLastFive() {
        synchronized (messageHistory) {
            int size = messageHistory.size();
            if (size >= 5)
                return messageHistory.subList(size - 5, size);
            else
                return messageHistory;
        }
    }

    public List<String> getUserList() {
        List<String> rst = null;
        synchronized (members) {
            rst = new ArrayList<>(members.keySet());
        }
        return rst;

    }

    public void sendMessage(Message msg) {
        /* Send message to the whole clique (including myself) */

        synchronized (members) {
            for (String userID : members.keySet()) {
                if (AddressBook.contains(userID)) {
                    InetSocketAddress dest = AddressBook.lookup(userID);

                    // send via communicator
                    comm.send(dest, msg);
                } else {
                    System.err.printf("Cannot send message to user %s. They are not in the address book.\n", userID);
                }
            }
        }
    }

    public void messageReceived(Message msg) {
        /* Callback received from Client class */
        if(msg instanceof UserAddedNotificationMessage) { // notification about a new added member
            String newUserID = ((UserAddedNotificationMessage) msg).userID;
            synchronized (members) {
                members.put(newUserID, new User(newUserID)); // insert the new user into members hashmap
            }
        }
        else if(msg instanceof InviteResponseMessage) { // response to an InviteMessage sent by me earlier
            boolean isValid = pendingInvites.contains(msg.author); // check if I actually invited this person
            boolean isAccept = ((InviteResponseMessage) msg).isAccept; //what they replied (yes/no)

            if(isValid && isAccept) { // if response valid and reply is positive
                String newUserID = msg.author;
                // notify everyone else in clique about new user
                synchronized (members) {
                    for(String peer: members.keySet()) {
                        InetSocketAddress peerAddr = AddressBook.lookup(peer); // get address of peer

                        comm.send(peerAddr, new UserAddedNotificationMessage(newUserID, client.getUserID(), this.name));
                    }

                    members.put(newUserID, new User(newUserID));
                }
            }
        }
        else if(msg instanceof TextMessage) { // it's just a simple message, insert into history
            synchronized (messageHistory) {
                messageHistory.add((TextMessage) msg);
            }
        }
    }



}
