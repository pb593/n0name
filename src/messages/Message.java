package messages;

import java.io.Serializable;

/**
 *  Base class for all possible types of messages.
 **/

public class Message implements Serializable{

    public String str;

    static final long serialVersionUID = 1L;

    public Message(String str){
        this.str = str;
    }

}
