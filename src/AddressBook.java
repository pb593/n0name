import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * Created by pb593 on 20/12/2015.
 */

public class AddressBook {


    private static final String servUrl = "http://pberkovich1994.pythonanywhere.com";
    private static final Integer REFRESH_RATE = 5; // refresh rate for the book (in seconds)


    private static HashMap<String, InetSocketAddress> book = pull();
    private static long timeLastUpdate = System.currentTimeMillis();


    synchronized public static void checkin(String userID, Integer port) {

        String myAddress = null; // get my IP address
        try {
            myAddress = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e) {
            Main.logger.severe("Unable to retrieve my IP address to report to address server.");
            return;
        }
        String urlToRead = servUrl + "/check-in/" + userID + "/" + myAddress + "/" + port.toString();
        HTTPHandler.httpGetRequest(urlToRead); // send a GET request to this URL

        // now book has definitely changed, force an update
        book = pull();

    }

    synchronized public static InetSocketAddress lookup(String userID) {

        book = getAll(); // update if necessary

        String urlToRead = servUrl + "/lookup/" + userID;
        String response = HTTPHandler.httpGetRequest(urlToRead);

        if(response.equals("None")) // userID not present in the book
            return null;
        else { // address is here
            String[] tokens = response.split(":");
            InetSocketAddress addr = new InetSocketAddress(tokens[0], Integer.parseInt(tokens[1])); // error handling?
            return addr;
        }

    }

    synchronized public static boolean contains(String userID) {

        book = getAll(); // update book if necessary

        InetSocketAddress addr = lookup(userID);
        return (addr != null);
    }

    synchronized public static HashMap<String, InetSocketAddress> getAll() {

        long now = System.currentTimeMillis();
        if(now - timeLastUpdate > REFRESH_RATE * 1000) { // book older than 5 seconds
            book = pull(); // update the book
            timeLastUpdate = now; // update the timestamp
        }

        return book;

    }

    private static HashMap<String, InetSocketAddress> pull() {

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
