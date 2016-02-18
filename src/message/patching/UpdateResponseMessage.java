package message.patching;

import message.Message;
import message.TextMessage;
import org.json.simple.JSONObject;

import java.util.List;

/**
 * Created by pb593 on 18/02/2016.
 */
public class UpdateResponseMessage extends Message {

    List<TextMessage> missingMessages;

    public UpdateResponseMessage(List<TextMessage> missingMessages, String author, String cliqueName) {
        super(author, cliqueName);
        this.missingMessages = missingMessages;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject obj = super.startJSON();

        obj.put("msg_type", this.getClass().getSimpleName());
        // TODO

        return obj;

    }
}
