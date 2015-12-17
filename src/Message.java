import java.io.Serializable;

/**
 *  Base class for all possible types of messages.
 **/

public class Message implements Serializable{

    public final String msg; //message text
    public final String author; //userID
    public final String cliqueName; // name of clique to which message belongs

    static final long serialVersionUID = 1L;

    public Message(String msg, String author, String cliqueName)
    {
        this.msg = msg;
        this.author = author;
        this.cliqueName = cliqueName;
    }
}
