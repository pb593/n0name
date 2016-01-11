package message;

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

}
