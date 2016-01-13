package message;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * Created by pb593 on 11/01/2016.
 */
public class InviteResponseMessage extends Message implements Serializable {

    public final boolean isAccept;
    public final BigInteger pubKey;

    public InviteResponseMessage(boolean isAccept, BigInteger pubKey, String author, String cliqueName) {
        super(author, cliqueName);
        this.isAccept = isAccept;
        this.pubKey = pubKey;
    }
}
