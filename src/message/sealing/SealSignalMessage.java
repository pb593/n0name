package message.sealing;

import message.Message;
import org.json.simple.JSONObject;

/**
 * Created by pb593 on 27/02/2016.
 */
public class SealSignalMessage extends Message {

    public final String fingerprint;

    public SealSignalMessage(String fingerprint, String author, String cliqueName) {
        super(author, cliqueName);
        this.fingerprint = fingerprint;
    }

    public SealSignalMessage(JSONObject obj) {
        super((String)obj.get("author"), (String)obj.get("cliqueName"));
        this.fingerprint = (String)obj.get("fingerprint");
    }

    @Override
    public JSONObject toJSON() {

        JSONObject obj = super.startJSON();
        obj.put("msg_type", this.getClass().getSimpleName());
        obj.put("fingerprint", fingerprint);

        return obj;
    }
}
