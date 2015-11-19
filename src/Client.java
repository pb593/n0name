/**
 * Created by pb593 on 19/11/2015.
 */

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Client extends Thread {

    private final String name;
    private final int port;
    private final Communicator comm;
    private final HashMap<String, Clique> cliques = new HashMap<>();

    public Client(String name, int port) {
        this.name = name;
        this.port = port;

        Communicator commtmp = null;
        try {
            commtmp = new Communicator(this, port);
            // start communicator, giving it a reference back to Client
        }
        catch (IOException e) { // can't bind to the port (usually because it's taken)
            System.err.printf("Client with name = %s unable to bind to port = %d", name, port);
            System.exit(-1); // just terminate
        }
        comm = commtmp;
    }


    @Override
    public void run() { //
        // this will probably become the main func in the final release
        //will probably be showing the CLI here
        comm.start(); //start our communicator
    }

    public void msgReceived(Message msg) {
        //callback from the Communicator (always happens in a separate thread)
        String str = msg.str;
        String[] tokens = str.split("|");
        String msgType = tokens[0];
        if(msgType.equals("Invite")){
            String cliqueName = tokens[1];
            Clique c = cliques.get(cliqueName);
            // TODO
        }
        else if(msgType.equals("InviteResp")) {
            // TODO
        }
        else {
            System.err.printf("A with unknown prefix %s has been received by %s at port %d\n",
                    msgType, this.name, this.port);
            System.exit(-1);
        }
    }

    public boolean startClique(String name, Set<User> users) {
        Clique c = new Clique(name);
        User me = new User(name, "localhost:" + String.valueOf(port)); //add myself to clique
        c.addUser(me); //add myself to clique
        cliques.put(name, c);
        for(User u: users) { // invite peers to the conversation, one-by-one
            c.inviteUser(u, comm); //send out an invite message
        }
        return true; //TODO
    }

    private String hashName(List<String> strs) {
        //computes a hash name for a clique, based on host addresses
        StringBuffer buf = new StringBuffer();
        for(String s: strs)
            buf.append(s);
        return String.valueOf(buf.toString().hashCode());
    }


}
