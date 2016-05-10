package scaffolding;

import exception.MessengerOfflineException;

import java.util.Arrays;
import java.util.List;

/**
 * Created by pb593 on 28/01/2016.
 */
public class StoreAndForward {

    public static final Integer SAF_REFRESH_RATE = 1000; // period of checking store-n-forward for new messages (ms)

    private static final String servUrl = "http://pberkovich1994.pythonanywhere.com/saf/";

    public static synchronized boolean send(String userID, String msg) throws MessengerOfflineException {

        String response = HTTPHandler.httpPostRequest(servUrl + "store/" + userID, msg);

        return response.equals("ACK"); // return true if ACKED

    }

    public static synchronized List<String> retrieve(String userID) throws MessengerOfflineException {

        String urlToRead = servUrl + "retrieve/" + userID;
        String response = HTTPHandler.httpGetRequest(urlToRead);
        if(response.startsWith("None"))
            return null;
        else
            return Arrays.asList(response.split("\n"));

    }

}
