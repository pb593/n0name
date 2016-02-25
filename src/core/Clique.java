package core;

import message.*;
import message.patching.UpdateRequestMessage;
import message.patching.UpdateResponseMessage;
import scaffolding.AddressBook;
import scaffolding.Utils;

import java.math.BigInteger;
import java.util.*;

/**
 * Created by pb593 on 19/11/2015.
 */

public class Clique extends Thread {

    public final String name;
    private final HashMap<String, User> members = new HashMap<>();
    private final MessageHistory history = new MessageHistory(); // messages in the order of arrival
    private final Client client;
    private final Communicator comm;
    private final HashSet<String> pendingInvites = new HashSet<>(); // users I invited and waiting for response
    private final HashSet<String> pendingPatchRequests = new HashSet<>(); // users I have sent a patch request to
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
        String toTransmit = "NoNaMe" + invRespMsg.toJSON().toJSONString();

        comm.send(invmsg.author, toTransmit); // reply, accepting the invitation and sending my pubkey

        crypto.acceptPublicKey(invmsg.pubKey); // update the common secret
        client.addAddressTag(getCurrentAddressTag(), this.name); // publish the new address tag
    }

    @Override
    public void run() {
        // active code of the Clique
        // Goals:
        //      1. participate in patching process
        //      2. seal off blocks
        Random rn = new Random();
        while(true) {
            // TODO: might need to tweak the delays later
            int delay = 1000 + rn.nextInt(4000); // between 1000 and 5000 ms
            Utils.sleep(delay);

            // TODO: more fine-grained locking?
            // issue a randomly spaced burst of patch requests
            synchronized (members) { // get a lock for memebers
                for(String userID: members.keySet()) {
                    synchronized (pendingPatchRequests) { // get a lock on pending patch requests
                        if (!userID.equals(this.client.getUserID()) && !pendingPatchRequests.contains(userID)) {
                            UpdateRequestMessage urm = new UpdateRequestMessage(history.getVectorClk(),
                                    client.getUserID(), this.name);
                            String toTransmit = encryptAndMac(urm);
                            comm.send(userID, toTransmit);
                            pendingPatchRequests.add(userID);
                            // System.out.printf("Clique: sent UpdateRequestMessage to user %s\n", userID);
                            Utils.sleep(rn.nextInt(500)); // sleep 0 to 500 ms
                        }
                    }
                }
            }


        }

    }

    public void addMember(String userID) {

        if(AddressBook.contains(userID)) {

            Message invMsg = null;
            synchronized (members) {
                invMsg = new InviteMessage(members.keySet(), crypto.getPublicKey(),
                        client.getUserID(), this.name);

            }

            String toTransmit = "NoNaMe" + invMsg.toJSON().toJSONString();
            // send the invite
            comm.send(userID, toTransmit);
            // mark invite as pending response
            pendingInvites.add(userID);
        }
        else {
            System.err.printf("Unable to add user '%s' to group. User is not in address book.\n", userID);
        }
    }

    public String getCliqueName() {
        return name;
    }

    public List<TextMessage> getLastFive() {
        return history.getLastFive();
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

        if(msg instanceof TextMessage) {
            TextMessage txtMsg = (TextMessage) msg;
            timestamp(txtMsg); // timestamp the message
            history.insert(txtMsg);
        }
        else {
            synchronized (members) {
                for (String userID : members.keySet()) {
                    String toTransmit = encryptAndMac(msg);
                    comm.send(userID, toTransmit);
                }
            }
        }
    }

    public void datagramReceived(String datagram) {
        String mac = datagram.substring(0, Cryptographer.macB64StringLength);
        String encMsg = datagram.substring(Cryptographer.macB64StringLength);

        //verify the MAC
        if(!mac.equals(crypto.Mac(this.name + encMsg))) { // if MACs do not match
            System.err.println("Received message failed to pass integrity check");
            return; // just drop it if message invalid
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
                                    encryptAndMac(new UserAddedNotificationMessage(newUserID,
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
        else if(msg instanceof UpdateRequestMessage) { // patching request
            UpdateRequestMessage urm = (UpdateRequestMessage) msg;
            VectorClock otherVC = urm.vectorClk;

            //System.out.printf("Received an UpdateRequestMessage from %s. Their vc:", urm.author);
            //otherVC.print();

            //form a response
            List<TextMessage> missingMsgs = history.getMissingMessages(otherVC);

            /*
            if(!missingMsgs.isEmpty()) {
                System.out.printf("missingMsgs:\n");
                for (TextMessage t : missingMsgs) {
                    System.out.printf("\t%s\n", t.text);
                }
                System.out.println();
            } */


            UpdateResponseMessage resp = new UpdateResponseMessage(missingMsgs, client.getUserID(), this.name);
            String toTransmit = encryptAndMac(resp);
            comm.send(urm.author, toTransmit); // reply to the author
            // System.out.println("Successfully sent an UpdateResponseMessage");
        }
        else if(msg instanceof UpdateResponseMessage) {
            UpdateResponseMessage resp = (UpdateResponseMessage)msg;
            // System.out.printf("Received an UpdateResponseMessage from %s\n", resp.author);

            // verify that this is a legit response to an actually sent request
            synchronized (pendingPatchRequests) {
                if(!pendingPatchRequests.contains(resp.author)) {// if I did not send a patch request
                    // System.out.println("Nope, I did not ask this user for an update. Dropping this.");
                    return; // just ignore this message
                }
                else {
                    // System.out.println("Yep, I asked for an update.");
                    pendingPatchRequests.remove(resp.author); // remove if response is legit
                }
            }


            List<TextMessage> missing = resp.missingMessages;
            /*
            if(!missing.isEmpty()) {
                System.out.printf("Missing messages from newly arrived UpdateResponse:\n");
                for (TextMessage t : missing) {
                    System.out.printf("\t%s\n", t.text);
                }
                System.out.println();
            } */



            history.insertAll(missing); // insert into history
        }
        else { // some unknown message type
            return; //just drop it
        }
    }

    private void timestamp(TextMessage txtMsg) {
        txtMsg.lamportTime = history.getCurrentLamportTS(); // set to my Lamport ts
    }

    private String encryptAndMac(Message msg) {

        String cliqueTag = crypto.Mac(this.name); // MAC(K, cliqueName)
        String encString = crypto.encryptMsg(msg); // ENC(K, msg)
        String mac = crypto.Mac(this.name + encString); // MAC(K, cliqueName || ENC(K, msg))

        // MSG_LAYOUT: MAC(K, cliqueName) || MAC(K, cliqueName || ENC(K, msg)) || ENC(K, msg)

        return cliqueTag +
                mac +
                encString;

    }

    public String getCurrentAddressTag() {
        String cliqueTag = crypto.Mac(this.name);
        return cliqueTag;
    }



}
