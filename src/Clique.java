import message.*;

import java.math.BigInteger;
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
    private final Cryptographer crypto = new Cryptographer("AES", 256);

    public Clique(String name, Client client, Communicator comm) {
        this.name = name;
        this.client = client;
        this.comm = comm;

        // put myself in the Clique
        synchronized (members) {
            members.put(client.getUserID(), new User(client.getUserID()));
        }
    }

    public Clique(String name, Client client, Communicator comm, InviteMessage invmsg) {
        // this constructor is used if a third party adds me to the clique

        this.name = name;
        this.client = client;
        this.comm = comm;

        // put all users into the members list (including myself)
        synchronized (members) {
            members.put(client.getUserID(), new User(client.getUserID()));
            for (String username : invmsg.userList) {
                members.put(username, new User(username));
            }
        }

        comm.send(invmsg.author,
                new InviteResponseMessage(true, this.crypto.getPublicKey(), client.getUserID(), this.name));
                                                            // reply, accepting the invitation and sending my pubkey

        crypto.acceptPublicKey(invmsg.pubKey); // update the common secret
    }

    public void addMember(String userID) {

        if(AddressBook.contains(userID)) {

            // send the invite
            synchronized (members) {
                comm.send(userID, new InviteMessage(members.keySet(), crypto.getPublicKey(),
                                                                        client.getUserID(), this.name));
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
                comm.send(userID, msg);
            }
        }
    }

    public void messageReceived(Message msg) {
        /* Callback received from Client class */
        if(msg instanceof UserAddedNotificationMessage) { // notification about a new added member
            String newUserID = ((UserAddedNotificationMessage) msg).userID;
            BigInteger newUserPubKey = ((UserAddedNotificationMessage) msg).pubKey;

            synchronized (members) {
                members.put(newUserID, new User(newUserID)); // insert the new user into members hashmap
            }

            // update the common secret with new user's pubkey
            crypto.acceptPublicKey(newUserPubKey);
        }
        else if(msg instanceof InviteResponseMessage) { // response to an InviteMessage sent by me earlier
            boolean isValid = pendingInvites.contains(msg.author); // check if I actually invited this person

            InviteResponseMessage invrespmsg = ((InviteResponseMessage) msg); // cast
            boolean isAccept = invrespmsg.isAccept; //what they replied (yes/no)

            if(isValid && isAccept) { // if response valid and reply is positive
                String newUserID = invrespmsg.author;
                BigInteger newUserPubKey = invrespmsg.pubKey;

                // notify everyone else in clique (except myself) about new user
                synchronized (members) {
                    for(String peer: members.keySet()) {

                        if(!peer.equals(this.client.getUserID())) {

                            comm.send(peer, new UserAddedNotificationMessage(newUserID, newUserPubKey,
                                    client.getUserID(), this.name));

                        }

                    }

                    members.put(newUserID, new User(newUserID)); // add new user to clique
                }

                // update the common secret with new user's pubkey
                crypto.acceptPublicKey(newUserPubKey);

            }
        }
        else if(msg instanceof TextMessage) { // it's just a simple message, insert into history
            synchronized (messageHistory) {
                messageHistory.add((TextMessage) msg);
            }
        }
    }



}
