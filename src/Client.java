/**
 * Created by pb593 on 19/11/2015.
 */

import messages.DHMessage;
import messages.Message;

import java.io.IOException;
import java.util.*;

public class Client implements Runnable {

    private final String name;
    private final int port;
    private final Communicator comm;
    private final HashMap<String, Clique> cliques = new HashMap<>();
    private final Crypto crypto = new Crypto();

    public Client(String name) {
        this.name = name;

        Random rnd = new Random();
        int port = 0; // prepare to choose randomly
        Communicator commtmp = null;
        while(true) { //look for a free port
            port = 50000 + rnd.nextInt(10000); //choose a random port between 50k and 60k
            try {
                commtmp = new Communicator(this, port); //try to bind to port
                // start communicator, giving it a reference back to Client
            } catch (IOException e) { // can't bind to the port
                Main.logger.config("Communicator unable to bind to port " + port + ". Looking for another one.");
                continue;
            }
            break;
        }
        this.port = port;
        comm = commtmp;
    }


    @Override
    public void run() { //
        // main function of Client
        comm.start(); //start our communicator
        Scanner scanner = new Scanner(System.in);
        while(true) {
            System.out.println("Enter msg in format 'host:port|msg':");
            String str =scanner.nextLine();
            if(str.equals("exit"))
                break;
            else {
                String[] tokens = str.split("\\|");
                String dest = tokens[0], msg = tokens[1];
                String[] hostport = dest.split(":");
                // do DH key agreement with target host
                try {
                    byte[] myPubKey = crypto.generatePubKey();
                    DHMessage dhmsg = new DHMessage(myPubKey);
                    comm.send(hostport[0], Integer.parseInt(hostport[1]), dhmsg);
                    // wait for DH response
                    while(true) {
                        synchronized (crypto.commonSecret) {
                            if (crypto.commonSecret != null)
                                break;
                        }
                    }
                    // now common secret has been established
                } catch (Exception e) {
                    e.printStackTrace();
                }


                comm.send(hostport[0], Integer.parseInt(hostport[1]), new Message(
                                                                        String.format("[%s] %s", this.name, msg)));
            }
        }
    }

    public void msgReceived(Message msg) {
        System.out.printf("Received message: '%s'\n", msg.str);

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
