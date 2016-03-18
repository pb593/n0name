package core;

import message.*;
import message.patching.UpdateRequestMessage;
import message.patching.UpdateResponseMessage;
import message.sealing.SealSignalMessage;
import scaffolding.AddressBook;
import scaffolding.Utils;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by pb593 on 19/11/2015.
 */

public class Clique extends Thread {

    public final String name; // group of clique / group
    private final Client client; // reference back to correponding client instance
    private final Communicator comm; // reference to the communicator
    private final Cryptographer crypto = new Cryptographer(); // reference to clique-specific instance of crpyto

    private final ConcurrentHashMap<String, User> members = new ConcurrentHashMap<>(); // group members
    private final MessageHistory history = new MessageHistory(); // messages (most recent first)

    private final HashSet<String> pendingInvites = new HashSet<>(); // users I invited and waiting for response
    private final ConcurrentHashMap<String, Long> pendingPatchRequests = new ConcurrentHashMap<>(); // users I have sent a patch request to
    private final HashMap<String, Set<String>> pendingBlockSeals = new HashMap<>();
                                // block fingerprint -> set of users who still need to confirm the sealing operation

    public Clique(String name, Client client, Communicator comm) {
        this.name = name;
        this.client = client;
        this.comm = comm;

        // put myself in the Clique
        members.put(client.getUserID(), new User(client.getUserID()));
        client.addAddressTag(getCurrentAddressTag(), this.name);
    }

    public Clique(String name, Client client, Communicator comm, InviteMessage invmsg) {
        // this constructor is used if a third party adds me to the clique

        this.name = name;
        this.client = client;
        this.comm = comm;

        // put myself into members map
        members.put(client.getUserID(), new User(client.getUserID()));

        // form an invite response
        Message invRespMsg = new InviteResponseMessage(true, this.crypto.getDHPublicKey(), client.getUserID(), this.name);
        String toTransmit = "NoNaMe" + invRespMsg.toJSON().toJSONString();

        comm.send(invmsg.author, toTransmit); // reply, accepting the invitation and sending my pubkey

        crypto.acceptDHPublicKey(invmsg.pubKey); // update the common secret
        client.addAddressTag(getCurrentAddressTag(), this.name); // publish the new address tag
    }

    @Override
    public void run() {
        // active code of the Clique
        // Goals:
        //      1. participate in patching process
        //      2. seal off blocks

        // various time parameters of the patching algo (in seconds)
        // TODO: might need to tweak the delays later
        final int PATCH_REQUEST_PERIOD_LOW = 1;
        final int PATCH_REQUEST_PERIOD_HIGH = 5;
        final int PATCH_REQUEST_TIMEOUT = 10;

        Random rn = new Random();
        while(true) {
            int delay = PATCH_REQUEST_PERIOD_LOW * 1000 +
                    rn.nextInt((PATCH_REQUEST_PERIOD_HIGH - PATCH_REQUEST_PERIOD_LOW) * 1000);
                                                                                        // between 1000 and 5000 ms
            Utils.sleep(delay);

            // clean up expired patch requests from pendingPatchRequests
            for (Iterator<Map.Entry<String, Long>> it = pendingPatchRequests.entrySet().iterator(); it.hasNext(); ){
                Map.Entry<String, Long> entry = it.next();
                if (System.currentTimeMillis() - entry.getValue() > PATCH_REQUEST_TIMEOUT * 1000) { // if expired
                    it.remove(); // delete
                }
            }

            // issue a randomly spaced burst of patch requests
            for(String userID: members.keySet()) {
                if (!userID.equals(this.client.getUserID()) && !pendingPatchRequests.containsKey(userID)) {
                    UpdateRequestMessage urm = new UpdateRequestMessage(history.getVectorClk(),
                            client.getUserID(), this.name);
                    String toTransmit = encryptAndMac(urm);
                    comm.send(userID, toTransmit);
                    pendingPatchRequests.put(userID, System.currentTimeMillis());
                    Utils.sleep(rn.nextInt(500)); // sleep 0 to 500 ms
                }
            }


            // try to find a sealable block
            SealableBlock sBlock = history.getNextSealableBlock(members.keySet());
            if(sBlock != null) { // if found one
                // TODO: sleeping inside synchronized block...
                synchronized (pendingBlockSeals) {
                    Set<String> haventConfirmed = pendingBlockSeals.get(sBlock.fingerprint);
                    if (haventConfirmed == null) {
                        haventConfirmed = new HashSet<>(members.keySet());
                        haventConfirmed.remove(client.getUserID()); //remove myself
                        pendingBlockSeals.put(sBlock.fingerprint, haventConfirmed); // add all users
                    }

                    if(haventConfirmed.isEmpty()) { // can seal the block now
                        history.sealNextBlock(members.keySet()); // seal it!
                        pendingBlockSeals.remove(sBlock.fingerprint); // remove it from pending hash table

                        /* update the secret and key */
                        String oldAddressTag = getCurrentAddressTag(); // note the current address tag
                        crypto.rotateKey(sBlock); // rotate the key
                        client.addAddressTag(getCurrentAddressTag(), this.name); // add new address tag
                        client.removeAddressTag(oldAddressTag); // remove the previous address tag


                    }

                    // issue a randomly spaced burst of seal signals to those who still has not confirmed seal
                    SealSignalMessage ssm = new SealSignalMessage(sBlock.fingerprint, client.getUserID(), this.getCliqueName());
                    String toTransmit = encryptAndMac(ssm); // prepare message to transmit
                    for (String userID : haventConfirmed) {
                        comm.send(userID, toTransmit); // send
                        Utils.sleep(rn.nextInt(500)); // sleep 0 to 500 ms
                    }
                }
            }


        }

    }

    public void addMember(String userID) {

        if(AddressBook.contains(userID)) {

            Message invMsg = new InviteMessage(members.keySet(), crypto.getDHPublicKey(),
                                                                                    client.getUserID(), this.name);


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
        List<String> rst = new ArrayList<>(members.keySet());
        return rst;

    }

    public void sendMessage(Message msg) {
        /* Send message to the whole clique (including myself) */

        if(msg instanceof TextMessage) {
            TextMessage txtMsg = (TextMessage) msg;
            timestamp(txtMsg); // timestamp the message
            history.insertMyNewMessage(txtMsg);
        }
        else {
            for (String userID : members.keySet()) {
                String toTransmit = encryptAndMac(msg);
                comm.send(userID, toTransmit);
            }
        }
    }

    public void datagramReceived(String datagram) {
        /* Callback received from Client class */

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
        if(msg instanceof MembershipUpdateMessage) { // secure update on group membership after I've been added
            MembershipUpdateMessage mum = (MembershipUpdateMessage) msg;

            for(String userID: mum.members) {
                if(!this.members.containsKey(userID)) // if don't have this user yet
                    this.members.put(userID, new User(userID)); // add them in !
            }

        }
        if(msg instanceof UserAddedNotificationMessage) { // notification about a new added member
            String newUserID = ((UserAddedNotificationMessage) msg).userID;
            BigInteger newUserPubKey = ((UserAddedNotificationMessage) msg).pubKey;

            members.put(newUserID, new User(newUserID)); // insert the new user into members hashmap

            String oldAddressTag = getCurrentAddressTag();

            // update the common secret with new user's pubkey
            crypto.acceptDHPublicKey(newUserPubKey);

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
                for(String peer: members.keySet()) {

                    if(!peer.equals(this.client.getUserID())) {
                        String toTransmit =
                                encryptAndMac(new UserAddedNotificationMessage(newUserID,
                                                                    newUserPubKey, client.getUserID(), this.name));
                        comm.send(peer, toTransmit);

                    }

                }

                members.put(newUserID, new User(newUserID)); // add new user to clique

                String oldAddressTag = getCurrentAddressTag();

                // update the common secret with new user's pubkey
                crypto.acceptDHPublicKey(newUserPubKey);
                // rotate the address tags
                client.addAddressTag(getCurrentAddressTag(), this.name); // add new address tag
                client.removeAddressTag(oldAddressTag); // remove the previous address tag

                // give the new user the list of current conversation participants (over secure channel)
                MembershipUpdateMessage mum = new MembershipUpdateMessage(new ArrayList(members.keySet()),
                                                                                        client.getUserID(), this.name);
                String toTransmit = encryptAndMac(mum);
                comm.send(invrespmsg.author, toTransmit);

            }
        }
        else if(msg instanceof UpdateRequestMessage) { // patching request
            UpdateRequestMessage urm = (UpdateRequestMessage) msg;
            VectorClock otherVC = urm.vectorClk;

            //form a response
            List<TextMessage> missingMsgs = history.getMissingMessages(otherVC);

            UpdateResponseMessage resp = new UpdateResponseMessage(missingMsgs, client.getUserID(), this.name);
            String toTransmit = encryptAndMac(resp);
            comm.send(urm.author, toTransmit); // reply to the author
        }
        else if(msg instanceof UpdateResponseMessage) {
            UpdateResponseMessage resp = (UpdateResponseMessage)msg;

            // verify that this is a legit response to an actually sent request
            if(!pendingPatchRequests.containsKey(resp.author)) // if I did not send a patch request
                return; // just ignore this message
            else
                pendingPatchRequests.remove(resp.author); // remove if response is legit


            List<TextMessage> missing = resp.missingMessages;
            history.insertPatch(missing); // insert into history
        }
        else if(msg instanceof SealSignalMessage) {
            SealSignalMessage ssm = (SealSignalMessage) msg;

            synchronized (pendingBlockSeals) {
                if (pendingBlockSeals.containsKey(ssm.fingerprint)) { // if I've also found this block
                    // reply with my own confirmation
                    SealSignalMessage mySsm = new SealSignalMessage(ssm.fingerprint, client.getUserID(), getCliqueName());
                    String toTransmit = encryptAndMac(mySsm); // prepare message
                    comm.send(ssm.author, toTransmit); // send

                    // update the set of people who still need to confirm this block
                    Set<String> haventConfirmed = pendingBlockSeals.get(ssm.fingerprint);
                    haventConfirmed.remove(ssm.author); // mark this person as having confirmed the block
                } else
                    return; // I haven't found this block yet, just drop this message
            }
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

        return cliqueTag + mac + encString;

    }

    public String getCurrentAddressTag() {
        String cliqueTag = crypto.Mac(this.name);
        return cliqueTag;
    }



}
