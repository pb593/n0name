package message;

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

}
