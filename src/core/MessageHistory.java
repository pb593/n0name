package core;

import message.TextMessage;
import message.patching.TextMessageComparator;

import java.util.*;

/**
 * Created by pb593 on 18/02/2016.
 */
public class MessageHistory {

    private TreeSet<TextMessage> tail = new TreeSet<>(new TextMessageComparator());
        // later messages come up in the beginning (see comparator)

    private VectorClock vectorClk = new VectorClock();
    private Integer lamportTimestamp = 0;
    private Integer blocksSealedCount = 0;

    synchronized public void insertMyNewMessage(TextMessage txtMsg) {

        if(!tail.contains(txtMsg)) { // if msg is not already present in history
            tail.add(txtMsg); // add it

            // update the vector clock appropriately
            vectorClk.increment(txtMsg.author);

            // update lamport TS
            lamportTimestamp += 1; // increment on sending new message
        }

    }

    synchronized public void insertPatch(Collection<TextMessage> c) {

        int maxTS = 0;

        for(TextMessage txtMsg: c) {
            if(!tail.contains(txtMsg)) {
                tail.add(txtMsg);
                vectorClk.increment(txtMsg.author);
                if(txtMsg.lamportTime > maxTS) maxTS = txtMsg.lamportTime;
            }
        }

        lamportTimestamp = Math.max(maxTS, lamportTimestamp) + 1; // update on patch
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
        for(TextMessage msg: tail) {
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
        for(TextMessage msg: tail) {
            if(count >= 5) break;

            rst.add(0, msg);
            count++;
        }

        return rst;
    }

    synchronized public SealableBlock getNextSealableBlock(Set<String> memberSet) {

        if(memberSet.size() > vectorClk.size()) { // if suggested clique size is > number of entries in vectorClk
            return null;
        }

        // STAGE 1: Caclulate the sealable set
        HashSet<String> authorsToBeSeen = new HashSet<>(memberSet); // users we have not seen yet on the iteration
        TreeSet<TextMessage> sealableSet = null;

        Iterator<TextMessage> iter = tail.iterator(); // iterate from most recent to oldest
        while(iter.hasNext()) {
            TextMessage msg = iter.next(); // get message
            String author = msg.author; // get its author

            authorsToBeSeen.remove(author); // remove them from the set of unseen users

            if (authorsToBeSeen.isEmpty()) { // once we have seen everyone
                sealableSet = new TreeSet<>(tail.subSet(msg, true, tail.last(), true));
                break; // exit loop
            }

        }

        // if we iterated through the whole history and have not seen all authors, there is no sealable block atm
        if(!authorsToBeSeen.isEmpty())
            return null;

        // STAGE 2: Calculate the last block
        authorsToBeSeen = new HashSet<>(memberSet); // refresh the set of authors to be seen
        iter = sealableSet.descendingIterator(); // iterate from the end (oldest first) of the sealable set
        while(iter.hasNext()) {
            TextMessage msg = iter.next();
            String author = msg.author;


            authorsToBeSeen.remove(author); // mark author as seen

            if(authorsToBeSeen.isEmpty()) { // once we have seen everyone
                List<TextMessage> msgs = new ArrayList<>(sealableSet.subSet(msg, true, sealableSet.last(), true).descendingSet());
                return msgs.isEmpty() ? null : new SealableBlock(msgs, blocksSealedCount); // return null for empty blocks
            }


        }

        return null;


    }

    synchronized void sealNextBlock(Set<String> memberSet) {
        SealableBlock block = getNextSealableBlock(memberSet);
        // block.print();
        tail = new TreeSet<>(tail.headSet(block.lastMessage(), false));
        blocksSealedCount++;
    }

    synchronized public int getCurrentLamportTS() {
        return lamportTimestamp;
    }

    synchronized public VectorClock getVectorClk() {
        return vectorClk;
    }

    synchronized public Integer size() {
        return tail.size();
    }

}
