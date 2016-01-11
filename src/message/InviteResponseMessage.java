package message;

import java.io.Serializable;

/**
 * Created by pb593 on 11/01/2016.
 */
public class InviteResponseMessage extends Message implements Serializable {

    public final boolean isAccept;

    public InviteResponseMessage(boolean isAccept, String author, String cliqueName) {
        super(author, cliqueName);
        this.isAccept = isAccept;
    }
}
