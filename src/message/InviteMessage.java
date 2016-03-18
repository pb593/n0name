package message;

import org.json.simple.JSONObject;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Set;

/**
 * Created by pb593 on 11/01/2016.
 */
public class InviteMessage extends Message {

    public final BigInteger pubKey; // public key (G ^ secret mod P)


    public InviteMessage(Set<String> users, BigInteger pubKey ,String author, String cliqueName) {
        super(author, cliqueName); // call Message constructor
        this.pubKey = pubKey;
    }

    public InviteMessage(JSONObject json) {
        super((String)json.get("author"), (String)json.get("cliqueName"));
        pubKey = new BigInteger((String) json.get("publicKey"));
    }

    @Override
    public JSONObject toJSON() {

        JSONObject obj = super.startJSON();

        obj.put("msg_type", this.getClass().getSimpleName());
        obj.put("publicKey", pubKey.toString());

        return obj;
    }

}
