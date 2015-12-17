import com.sun.deploy.util.StringUtils;

import java.net.InetSocketAddress;
import java.util.*;

/**
 * Created by pb593 on 19/11/2015.
 */

public class Clique {

    private final String name;
    private final HashMap<String, User> members = new HashMap<>(); // contains all users except me
    private final List<Message> messageHistory = new ArrayList<>(); // messages in the order of arrival
    private final Client client;
    private final Communicator comm;

    public Clique(String name, Client client, Communicator comm) {
        this.name = name;
        this.client = client;
        this.comm = comm;

        // put myself in the Clique
        members.put(client.getUserID(), new User(client.getUserID()));
    }

    public Clique(String name, Client client, Communicator comm, String listOfUsers) {
        // this constructor is used if a third party adds me to the clique

        this.name = name;
        this.client = client;
        this.comm = comm;

        // put all users into the members list (including myself)
        members.put(client.getUserID(), new User(client.getUserID()));
        for(String username: listOfUsers.split(",")) {
            members.put(username, new User(username));
        }
    }

    public void addMember(String userID) {

        if(Main.addressBook.containsKey(userID)) {
            // get address of the destination
            InetSocketAddress dest = Main.addressBook.get(userID);

            // form a message, telling the dest what clique they've been added to, with what peers
            StringBuffer sb = new StringBuffer("__inviteMsg__|"); // init __addMember__ msg
            sb.append(StringUtils.join(members.keySet(), ",")); // add names of all users in group

            // send the invite
            comm.send(dest, new Message(sb.toString(), client.getUserID(), this.name));

            // notify everyone else in clique about new user
            for(String peer: members.keySet()) {
                InetSocketAddress peerAddr = Main.addressBook.get(peer); // get address of peer

                comm.send(peerAddr, new Message("__userAddedNotification__|" + userID, client.getUserID(), this.name));
            }


            members.put(userID, new User(userID));
        }
        else {
            System.err.printf("Unable to add user '%s' to group. User is not in address book.\n", userID);
        }
    }

    public String getName() {
        return name;
    }

    public List<Message> getLastFive() {
        synchronized (messageHistory) {
            int size = messageHistory.size();
            if (size >= 5)
                return messageHistory.subList(size - 5, size);
            else
                return messageHistory;
        }
    }

    public String getUserListString() {
        return StringUtils.join(members.keySet(), ",");
    }

    public void sendMessage(Message msg) {
        /* Send message to the whole clique (including myself) */

        for(String userID: members.keySet()) {
            if(Main.addressBook.containsKey(userID)) {
                InetSocketAddress dest = Main.addressBook.get(userID);

                // send via communicator
                comm.send(dest, msg);
            }
            else {
                System.err.printf("Cannot send message to user %s. They are not in the address book.\n", userID);
            }
        }
    }

    public void messageReceived(Message msg) {
        /* Callback received from Client class */
        if(msg.msg.startsWith("__userAddedNotification__")) { // notification about a new added member
            String newUserID = msg.msg.split("\\|")[1];
            synchronized (members) {
                members.put(newUserID, new User(newUserID));
            }
        }
        else { // it's just a simple message, insert into history
            synchronized (messageHistory) {
                messageHistory.add(msg);
            }
        }
    }



}
