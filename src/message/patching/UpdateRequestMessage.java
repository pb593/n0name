package message.patching;

import message.Message;

/**
 * Created by pb593 on 18/02/2016.
 */
public class UpdateRequestMessage extends Message {


    protected UpdateRequestMessage(String author, String cliqueName) {
        super(author, cliqueName);
    }

    @Override
    public String toJSON() {
        // TODO
        return null;
    }
}
