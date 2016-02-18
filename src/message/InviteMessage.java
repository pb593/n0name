package message;

import com.sun.deploy.util.StringUtils;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;

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

    public InviteMessage(JSONObject json) {
        super((String)json.get("author"), (String)json.get("cliqueName"));
        userList = Arrays.asList(((String) json.get("userList")).split(",\\s+"));
        pubKey = new BigInteger((String) json.get("publicKey"));
    }

    @Override
    public JSONObject toJSON() {

        JSONObject obj = super.startJSON();

        obj.put("msg_type", this.getClass().getSimpleName());
        obj.put("userList", StringUtils.join(userList, ", "));
        obj.put("publicKey", pubKey.toString());

        return obj;
    }

}
