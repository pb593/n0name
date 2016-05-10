package scaffolding;

import core.Cryptographer;

import java.util.UUID;

/**
 * Created by pb593 on 24/02/2016.
 */
public class Utils {

    public static int PATCH_PERIOD = 3000; // by default, once every 3 s, on average;

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis); // try to sleep
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1); // just crash if fail :-)
        }
    }

    public static String randomAlphaNumeric(int length) {
        return UUID.randomUUID().toString().replace("-", "").substring(0, length);
    }




}
