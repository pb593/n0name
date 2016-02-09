import message.*;

import java.math.BigInteger;
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
    private final Cryptographer crypto = new Cryptographer();

    public Clique(String name, Client client, Communicator comm) {
        this.name = name;
        this.client = client;
        this.comm = comm;

        // put myself in the Clique
        synchronized (members) {
            members.put(client.getUserID(), new User(client.getUserID()));
        }
        client.addAddressTag(getCurrentAddressTag(), this.name);
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

        Message invRespMsg = new InviteResponseMessage(true, this.crypto.getPublicKey(), client.getUserID(), this.name);
        String toTransmit = "NoNaMe" + invRespMsg.toJSON();

        comm.send(invmsg.author, toTransmit); // reply, accepting the invitation and sending my pubkey

        crypto.acceptPublicKey(invmsg.pubKey); // update the common secret
        client.addAddressTag(getCurrentAddressTag(), this.name); // publish the new address tag
    }

    public void addMember(String userID) {

        if(AddressBook.contains(userID)) {

            Message invMsg = null;
            synchronized (members) {
                invMsg = new InviteMessage(members.keySet(), crypto.getPublicKey(),
                        client.getUserID(), this.name);

            }

            String toTransmit = "NoNaMe" + invMsg.toJSON();
            // send the invite
            comm.send(userID, toTransmit);
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
                String toTransmit = prepareMsgForTransmission(msg);
                comm.send(userID, toTransmit);
            }
        }
    }

    public void datagramReceived(String datagram) {
        String mac = datagram.substring(0, Cryptographer.macB64StringLength);
        String encMsg = datagram.substring(Cryptographer.macB64StringLength);

        // verify the MAC
        if(!mac.equals(crypto.Mac(this.name + encMsg))) { // if MACs do not match
            System.err.println("Received message failed to pass integrity check");
            return;
        }

        Message msg = crypto.decryptMsg(encMsg);

        messageReceived(msg);
    }

    public void messageReceived(Message msg) {

        /* Callback received from Client class */
        if(msg instanceof UserAddedNotificationMessage) { // notification about a new added member
            String newUserID = ((UserAddedNotificationMessage) msg).userID;
            BigInteger newUserPubKey = ((UserAddedNotificationMessage) msg).pubKey;

            synchronized (members) {
                members.put(newUserID, new User(newUserID)); // insert the new user into members hashmap
            }

            String oldAddressTag = getCurrentAddressTag();

            // update the common secret with new user's pubkey
            crypto.acceptPublicKey(newUserPubKey);

            client.addAddressTag(getCurrentAddressTag(), this.name); // add new address tag
            client.removeAddressTag(oldAddressTag); // remove the previous address tag
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
                            String toTransmit =
                                    prepareMsgForTransmission(new UserAddedNotificationMessage(newUserID,
                                                                        newUserPubKey, client.getUserID(), this.name));
                            comm.send(peer, toTransmit);

                        }

                    }

                    members.put(newUserID, new User(newUserID)); // add new user to clique
                }

                String oldAddressTag = getCurrentAddressTag();

                // update the common secret with new user's pubkey
                crypto.acceptPublicKey(newUserPubKey);
                client.addAddressTag(getCurrentAddressTag(), this.name); // add new address tag
                client.removeAddressTag(oldAddressTag); // remove the previous address tag


            }
        }
        else if(msg instanceof TextMessage) { // it's just a simple message, insert into history
            synchronized (messageHistory) {
                messageHistory.add((TextMessage) msg);
            }
        }
    }

    private String prepareMsgForTransmission(Message msg) {

        String cliqueTag = crypto.Mac(this.name); // MAC(K, cliqueName)
        String encString = crypto.encryptMsg(msg); // ENC(K, msg)
        String mac = crypto.Mac(this.name + encString); // MAC(K, cliqueName || ENC(K, msg))

        // MSG_LAYOUT: MAC(K, cliqueName) || MAC(K, cliqueName || ENC(K, msg)) || ENC(K, msg)

        return cliqueTag + mac + encString;

    }

    public String getCurrentAddressTag() {
        String cliqueTag = crypto.Mac(this.name);
        return cliqueTag;
    }



}
