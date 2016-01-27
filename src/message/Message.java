package message;

import org.json.simple.JSONObject;

import java.io.Serializable;

/**
 *  Base class for all possible types of messages.
 **/

public abstract class Message implements Serializable {

    public final String author; //userID
    public final String cliqueName; // name of clique to which message belongs

    static final long serialVersionUID = 1L;


    abstract public String toJSON();

    protected JSONObject startJSON() {
        // method called by children of this class
        // creates a JSONObject with basic parameters

        JSONObject obj = new JSONObject();
        obj.put("author", author);
        obj.put("cliqueName", cliqueName);

        return obj;
    }

    protected Message(String author, String cliqueName) {
        this.author = author;
        this.cliqueName = cliqueName;
    }

}
