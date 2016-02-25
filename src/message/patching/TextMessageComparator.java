package message.patching;

import message.TextMessage;

import java.util.Comparator;

/**
 * Created by pb593 on 18/02/2016.
 */
public class TextMessageComparator implements Comparator<TextMessage> {

    @Override
    public int compare(TextMessage m1, TextMessage m2) {
        int lt1 = m1.lamportTime;
        int lt2 = m2.lamportTime;

        int d = lt1 - lt2; // difference in Lamport timestamps

        if(d != 0)
            return -d; // want later messages (bigger TS) to be first in the list
        else // d == 0
            return m2.hashCode() - m1.hashCode(); // break ties using hashCodes (returns 0 if hashCodes are same)

    }
}
