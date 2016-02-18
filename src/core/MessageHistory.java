package core;

import message.TextMessage;
import message.patching.TextMessageComparator;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by pb593 on 18/02/2016.
 */
public class MessageHistory {

    TreeSet<TextMessage> treeSet = new TreeSet<>(new TextMessageComparator());
        // later messages come up in the beginning (see comparator)


    synchronized public void insert(TextMessage txtMsg) {
        treeSet.add(txtMsg);
    }

    synchronized public List<TextMessage> getLastNBy(Integer n, String userID) {
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

    synchronized public Integer size() {
        return treeSet.size();
    }

}
