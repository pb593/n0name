package message;

import jdk.nashorn.internal.parser.JSONParser;
import org.json.simple.JSONObject;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * Created by pb593 on 11/01/2016.
 */
public class UserAddedNotificationMessage extends Message implements Serializable {

    public final String userID;
    public final BigInteger pubKey;

    public UserAddedNotificationMessage(String userID, BigInteger pubKey, String author, String cliqueName) {
        super(author, cliqueName);
        this.userID = userID;
        this.pubKey = pubKey;
    }

    public UserAddedNotificationMessage(JSONObject json) { // construct from JSON
        super((String)json.get("author"), (String)json.get("cliqueName"));
        this.userID = (String)json.get("userID");
        this.pubKey = new BigInteger((String)json.get("publicKey"));
    }

    @Override
    public String toJSON() {
        JSONObject obj = super.startJSON();

        obj.put("msg_type", this.getClass().getSimpleName());
        obj.put("userID", userID);
        obj.put("publicKey", pubKey.toString());

        return obj.toJSONString();

    }

}
