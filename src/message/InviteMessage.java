package message;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by pb593 on 11/01/2016.
 */
public class InviteMessage extends Message implements Serializable {

    public List<String> userList;
    public final BigInteger pubKey; // public key (G ^ secret mod P)


    public InviteMessage(Set<String> users, BigInteger pubKey ,String author, String cliqueName) {
        super(author, cliqueName); // call Message constructor
        userList = new ArrayList<>(users);
        this.pubKey = pubKey;
    }

}
