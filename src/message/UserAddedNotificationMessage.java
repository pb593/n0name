package message;

import java.io.Serializable;

/**
 * Created by pb593 on 11/01/2016.
 */
public class UserAddedNotificationMessage extends Message implements Serializable {

    public final String userID;

    public UserAddedNotificationMessage(String userID, String author, String cliqueName) {
        super(author, cliqueName);
        this.userID = userID;
    }

}
