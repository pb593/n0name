package message.patching;

import message.Message;

/**
 * Created by pb593 on 18/02/2016.
 */
public class UpdateResponseMessage extends Message {

    protected UpdateResponseMessage(String author, String cliqueName) {
        super(author, cliqueName);
    }

    @Override
    public String toJSON() {
        return null;
    }
}
