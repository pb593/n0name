package scaffolding;

import exception.MessengerOfflineException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 *
 * Thread-safe implementation of online address book.
 * Could return stale info. It definitely will if we go offline.
 *
 */

public class AddressBook {


    private static final String servUrl = "http://pberkovich1994.pythonanywhere.com";
    private static final Integer REFRESH_RATE = 3; // refresh rate for the book (in seconds)


    private static HashMap<String, InetSocketAddress> book;
    private static long timeLastUpdate = System.currentTimeMillis();

    synchronized public static void init() throws MessengerOfflineException {

        book = forceRefresh();

    }


    synchronized public static void checkin(String userID, Integer port) throws MessengerOfflineException {

        String myAddress = null; // get my IP address
        try {
            myAddress = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e) {
            System.err.print("Unable to retrieve my IP address to report to address server.");
            return;
        }
        String urlToRead = servUrl + "/check-in/" + userID + "/" + myAddress + "/" + port.toString();
        HTTPHandler.httpGetRequest(urlToRead); // send a GET request to this URL

        // book has definitely changed, force an update
        book = forceRefresh();

    }

    synchronized public static InetSocketAddress lookup(String userID) {

        getAll(); // update if necessary

        return book.get(userID); // return answer from the book

    }

    synchronized public static boolean contains(String userID) {

        getAll(); // update book if necessary

        InetSocketAddress addr = lookup(userID);
        return (addr != null);
    }

    synchronized public static HashMap<String, InetSocketAddress> getAll() {

        long now = System.currentTimeMillis();
        if(now - timeLastUpdate > REFRESH_RATE * 1000) { // book older than 5 seconds
            try {
                HashMap<String, InetSocketAddress> newbook = forceRefresh();
                book =  newbook; // update the book
                timeLastUpdate = now; // update the timestamp
            } catch (MessengerOfflineException e) { // can't get the data from server, since we are offline
                // fine, the data in our book will be stale for now...
                System.out.println("Cannot reach the address book server. Data may be stale...");
            }
        }

        return book;

    }

    private static HashMap<String, InetSocketAddress> forceRefresh() throws MessengerOfflineException {

        HashMap<String, InetSocketAddress> table = new HashMap<>();

        String urlToRead = servUrl + "/display";
        String response = HTTPHandler.httpGetRequest(urlToRead);

        if(!response.equals("")) { // if the table is not empty
            String[] lines = response.split("<br>");
            for (String line : lines) {
                String[] tokens = line.split("\\s+");
                String userID = tokens[0];
                String addrport = tokens[1];

                String[] subtokens = addrport.split(":");
                String addr = subtokens[0];
                Integer port = Integer.parseInt(subtokens[1]);

                table.put(userID, new InetSocketAddress(addr, port));
            }
        }


        return table;

    }


}
