package message.patching;

import core.VectorClock;
import message.Message;

/**
 * Created by pb593 on 18/02/2016.
 */
public class UpdateRequestMessage extends Message {

    public final VectorClock vectorClk;

    public UpdateRequestMessage(VectorClock clk, String author, String cliqueName) {
        super(author, cliqueName);
        vectorClk = clk;
    }

    @Override
    public String toJSON() {
        // TODO
        return null;
    }
}
