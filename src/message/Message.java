package message;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.Serializable;

/**
 *  Base class for all possible types of messages.
 **/

public abstract class Message implements Serializable {

    public final String author; //userID
    public final String cliqueName; // name of clique to which message belongs

    static final long serialVersionUID = 1L;

    protected Message(String author, String cliqueName) {
        this.author = author;
        this.cliqueName = cliqueName;
    }

    public static Message fromJSON(String json) throws ParseException {

        JSONParser parser = new JSONParser();
        JSONObject obj = (JSONObject)parser.parse(json);

        String msg_type = (String)obj.get("msg_type");

        if(msg_type.equals("InviteMessage"))
            return new InviteMessage(obj);
        else if(msg_type.equals("InviteResponseMessage"))
            return new InviteResponseMessage(obj);
        else if(msg_type.equals("TextMessage"))
            return new TextMessage(obj);
        else if(msg_type.equals("UserAddedNotificationMessage"))
            return new UserAddedNotificationMessage(obj);
        else
            throw new ParseException(0); // msg_type is something unexpected

    }


    abstract public String toJSON();

    protected JSONObject startJSON() {
        // method called by children of this class
        // creates a JSONObject with basic parameters

        JSONObject obj = new JSONObject();
        obj.put("author", author);
        obj.put("cliqueName", cliqueName);

        return obj;
    }


}
