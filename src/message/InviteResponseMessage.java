package message;

import org.json.simple.JSONObject;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * Created by pb593 on 11/01/2016.
 */
public class InviteResponseMessage extends Message implements Serializable {

    public final boolean isAccept;
    public final BigInteger pubKey;

    public InviteResponseMessage(boolean isAccept, BigInteger pubKey, String author, String cliqueName) {
        super(author, cliqueName);
        this.isAccept = isAccept;
        this.pubKey = pubKey;
    }

    public InviteResponseMessage(JSONObject json) {
        super((String)json.get("author"), (String) json.get("cliqueName"));
        this.isAccept = (boolean)json.get("isAccept");
        this.pubKey = new BigInteger((String) json.get("publicKey"));
    }


    @Override
    public String toJSON() {
        JSONObject obj = super.startJSON();

        obj.put("msg_type", this.getClass().getSimpleName());
        obj.put("isAccept", isAccept);
        obj.put("publicKey", pubKey.toString());

        return obj.toJSONString();

    }

}
