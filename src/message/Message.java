package message;

import interfaces.JSONizable;
import message.patching.UpdateRequestMessage;
import message.patching.UpdateResponseMessage;
import message.sealing.SealSignalMessage;
import message.sealing.SealResponseMessage;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *  Base class for all possible types of messages.
 **/

public abstract class Message implements JSONizable {

    public final String author; //userID
    public final String cliqueName; // name of clique to which message belongs

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
        else if(msg_type.equals("UpdateRequestMessage"))
            return new UpdateRequestMessage(obj);
        else if(msg_type.equals("UpdateResponseMessage"))
            return new UpdateResponseMessage(obj);
        else if(msg_type.equals("SealSignalMessage"))
            return new SealSignalMessage(obj);
        else if(msg_type.equals("SealResponseMessage"))
            return new SealResponseMessage(obj);
        else if(msg_type.equals("MembershipUpdateMessage"))
            return new MembershipUpdateMessage(obj);
        else
            throw new ParseException(0); // msg_type is something unexpected

    }

    protected JSONObject startJSON() {
        // method called by children of this class
        // creates a JSONObject with basic parameters

        JSONObject obj = new JSONObject();
        obj.put("author", author);
        obj.put("cliqueName", cliqueName);

        return obj;
    }


}
