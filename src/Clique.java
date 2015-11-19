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

        //rough solution, for testing
        StringBuffer buf = new StringBuffer();
        buf.append("Invite|");
        buf.append(name + "|");
        Integer exp = ((int)Math.pow(g, secret)) % p;
        buf.append(exp.toString());
        c.send(peerHost, peerPort, new Message(buf.toString())); //send invite, wait for response


    }

}
