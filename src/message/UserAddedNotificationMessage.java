package message;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Created by pb593 on 11/01/2016.
 */
public class UserAddedNotificationMessage extends Message implements Serializable {

    public final String userID;
    public final BigInteger pubKey;

    public UserAddedNotificationMessage(String userID, BigInteger pubKey, String author, String cliqueName) {
        super(author, cliqueName);
        this.userID = userID;
        this.pubKey = pubKey;
    }

}
