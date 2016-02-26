package core;

import message.TextMessage;
import message.patching.TextMessageComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by pb593 on 18/02/2016.
 */
public class MessageHistory {

    private TreeSet<TextMessage> treeSet = new TreeSet<>(new TextMessageComparator());
        // later messages come up in the beginning (see comparator)

    private VectorClock vectorClk = new VectorClock();
    private Integer lamportTimestamp = 0;


    synchronized public void insertMyNewMessage(TextMessage txtMsg) {

        if(!treeSet.contains(txtMsg)) { // if msg is not already present in history
            treeSet.add(txtMsg); // add it

            // update the vector clock appropriately
            vectorClk.increment(txtMsg.author);

            // update lamport TS
            lamportTimestamp += 1; // increment on sending new message
        }

    }

    synchronized public void insertPatch(Collection<TextMessage> c) {

        int maxTS = 0;

        for(TextMessage txtMsg: c) {
            treeSet.add(txtMsg);
            if(txtMsg.lamportTime > maxTS) maxTS = txtMsg.lamportTime;
        }

        lamportTimestamp = Math.max(maxTS, lamportTimestamp) + 1; // update on patch
        // System.out.printf("New Lamport TS: %d\n", lamportTimestamp);
    }

    synchronized public List<TextMessage> getMissingMessages(VectorClock otherVC) {
        List<TextMessage> result = new ArrayList<>();
        VectorClock delta = VectorClock.diff(this.vectorClk, otherVC);
        for(String userID: delta) {
            Integer Ndiff = delta.get(userID);
            result.addAll(getLastNBy(Ndiff, userID));
        }
        return result;

    }

    private List<TextMessage> getLastNBy(Integer n, String userID) {
        // TODO: inefficient, optimise!
        List<TextMessage> rst = new ArrayList<>();
        Integer count = 0;
        for(TextMessage msg: treeSet) {
            if(count >= n) break;

            if(msg.author.equals(userID)) {
                rst.add(0, msg); // add to beginning
                count++;
            }
        }

        return rst;
    }

    synchronized public List<TextMessage> getLastFive() {
        List<TextMessage> rst = new ArrayList<>();
        Integer count = 0;
        for(TextMessage msg: treeSet) {
            if(count >= 5) break;

            rst.add(0, msg);
            count++;
        }

        return rst;
    }

    synchronized public int getCurrentLamportTS() {
        return lamportTimestamp;
    }

    synchronized public VectorClock getVectorClk() {
        return vectorClk;
    }

    synchronized public Integer size() {
        return treeSet.size();
    }

}
