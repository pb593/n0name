package scaffolding;

/**
 * Created by pb593 on 24/02/2016.
 */
public class Utils {

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis); // try to sleep
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(-1); // just crash if fail
        }
    }



}
