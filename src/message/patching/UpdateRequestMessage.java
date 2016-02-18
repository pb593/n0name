package message.patching;

import core.VectorClock;
import message.Message;
import org.json.simple.JSONObject;

/**
 * Created by pb593 on 18/02/2016.
 */
public class UpdateRequestMessage extends Message {

    public final VectorClock vectorClk;

    public UpdateRequestMessage(VectorClock clk, String author, String cliqueName) {
        super(author, cliqueName);
        vectorClk = clk;
    }

    public UpdateRequestMessage(JSONObject json) {
        super((String)json.get("author"), (String)json.get("cliqueName"));
        vectorClk = new VectorClock((JSONObject)json.get("vectorClk"));
    }

    @Override
    public JSONObject toJSON() {

        JSONObject obj = super.startJSON();
        obj.put("msg_type", this.getClass().getSimpleName());
        obj.put("vectorClk", vectorClk.toJSON());

        return obj;
    }
}
