package message.patching;

import message.Message;
import message.TextMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pb593 on 18/02/2016.
 */
public class UpdateResponseMessage extends Message {

    public final List<TextMessage> missingMessages;

    public UpdateResponseMessage(List<TextMessage> missingMessages, String author, String cliqueName) {
        super(author, cliqueName);
        this.missingMessages = missingMessages;
    }

    public UpdateResponseMessage(JSONObject json) {
        super((String)json.get("author"), (String)json.get("cliqueName"));

        JSONArray jsonArray = (JSONArray) json.get("missingMessages");
        List<TextMessage> missingMessages = new ArrayList<>();

        for(int i = 0; i < jsonArray.size(); i++) {
            JSONObject txtMsgJson = (JSONObject)jsonArray.get(i);
            missingMessages.add(new TextMessage(txtMsgJson));
        }

        this.missingMessages = missingMessages;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject obj = super.startJSON();

        obj.put("msg_type", this.getClass().getSimpleName());

        JSONArray jsonArray = new JSONArray();
        for(TextMessage msg: missingMessages) {
            jsonArray.add(msg.toJSON());
        }
        obj.put("missingMessages", jsonArray);

        return obj;

    }
}
