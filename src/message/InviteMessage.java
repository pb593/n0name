package message;

import java.io.Serializable;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by pb593 on 11/01/2016.
 */
public class InviteMessage extends Message implements Serializable {

    public List<String> userList;

    public InviteMessage(Set<String> users, String author, String cliqueName) {
        super(author, cliqueName); // call Message constructor
        userList = new ArrayList<>(users);
    }

}
