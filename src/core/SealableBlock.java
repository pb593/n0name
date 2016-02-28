package core;

import message.TextMessage;

import java.util.List;

/**
 * Created by pb593 on 27/02/2016.
 */
public class SealableBlock {

    private final List<TextMessage> block;
    private final Integer blockNumber;
    public final VectorClock vectorClk = new VectorClock();
    public final String fingerprint;

    public SealableBlock(List<TextMessage> block, Integer blockNumber) {
        this.block = block;
        this.blockNumber = blockNumber;
        fingerprint = Cryptographer.digest(this.toString());

        // compute the vc of the block
        for(TextMessage msg: block) {
            vectorClk.increment(msg.author);
        }
    }

    public TextMessage lastMessage() {
        return block.get(block.size() - 1);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(blockNumber.toString());

        for(TextMessage msg: block) {
            sb.append("|");
            sb.append(msg.author);
            sb.append(":");
            sb.append(msg.text);
            sb.append("@");
            sb.append(msg.lamportTime.toString());
        } // += e.g. "|john:hello@32"

        return sb.toString();

    }

    public void print() {
        System.out.println("Sealable block:");
        for(TextMessage msg: block) {
            System.out.printf("\t- %s: %s\n", msg.author, msg.text);
        }
    }

}
