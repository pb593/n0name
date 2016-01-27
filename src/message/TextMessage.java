package message;

import org.json.simple.JSONObject;

import java.io.Serializable;

/**
 * Created by pb593 on 11/01/2016.
 */
public class TextMessage extends Message implements Serializable {

    public final String text; //message text

    public TextMessage(String text, String author, String cliqueName) {
        super(author, cliqueName); // call Message constructor
        this.text = text;
    }

    public TextMessage(JSONObject json) {
        super((String)json.get("author"), (String)json.get("cliqueName"));
        this.text = (String)json.get("text");
    }

    @Override
    public String toJSON() {
        JSONObject obj = super.startJSON();

        obj.put("msg_type", this.getClass().getSimpleName());
        obj.put("text", text);

        return obj.toJSONString();

    }


}
