package message.sealing;

import message.Message;
import org.json.simple.JSONObject;

/**
 * Created by pb593 on 27/02/2016.
 */
public class SealResponseMessage extends Message {

    public final String fingerprint;
    public final boolean didAgree;

    
    public SealResponseMessage(String fingerprint, boolean didAgree, String author, String cliqueName) {
        super(author, cliqueName);
        this.fingerprint = fingerprint;
        this.didAgree = didAgree;
    }

    public SealResponseMessage(JSONObject obj) {
        super((String)obj.get("author"), (String)obj.get("cliqueName"));
        this.fingerprint = (String) obj.get("fingerprint");
        this.didAgree = (boolean) obj.get("didAgree");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject obj = super.startJSON();
        obj.put("msg_type", this.getClass().getSimpleName());
        obj.put("fingerprint", fingerprint);
        obj.put("didAgree", didAgree);

        return obj;
    }
}
