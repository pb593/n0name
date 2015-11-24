import messages.Message;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Created by pb593 on 19/11/2015.
 */

public class Clique {

    private final String name;
    private final Random generator = new Random();
    private final int g = 5;
    private final int p = 23;
    private int secret = generator.nextInt(100);
    private final Set<User> users = new HashSet<>();

    public Clique(String name){
        this.name = name;
    }

    public void addUser(User u) {
        users.add(u);
    }

    public void inviteUser(User newuser, Communicator c) {
        String peerHost = newuser.address.split(":")[0];
        int peerPort = new Integer(newuser.address.split(":")[1]);

        // rough solution, for testing
        // MSG_STRUCT: CliqueName|MsgType|Payload
        StringBuffer buf = new StringBuffer();
        buf.append(name + "|");
        buf.append("Invite|");
        Integer exp = ((int)Math.pow(g, secret)) % p;
        buf.append(exp.toString());
        c.send(peerHost, peerPort, new Message(buf.toString())); //send invite, wait for response




    }

    public void msgReceived(String msg) {
        // callback for when a message arrvies for the clique
        // always happens in a separate thread
        String[] tokens = msg.split("|");
        String cliqueName = tokens[0];
        assert(cliqueName.equals(this.name)); //make sure msg is for this clique

        String msgType = tokens[1];
        if(msgType.equals("Invite")) {

        }
        else if(msgType.equals("InviteResp")){

        }
        else {
            System.err.printf("Received a message of unfamiliar type %s\n. Ignoring it.", msgType);
        }



    }

}
