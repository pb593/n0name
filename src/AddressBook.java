import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.HashMap;

/**
 * Created by pb593 on 20/12/2015.
 */

public class AddressBook {

    private static final String servUrl = "http://pberkovich1994.pythonanywhere.com";

    private static String httpGetRequest(String urlToRead) {

        String result = null;
        try {


            StringBuilder sb = new StringBuilder();


            URL url = new URL(urlToRead);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line + "\n");
            }
            rd.close();

            result = sb.toString().trim(); // response by the server


        }
        catch (IOException e) {
            Main.logger.severe("Unable to do a HTTP GET request to the address server...");
            System.exit(-1);
        }

        return result;

    }

    public static void checkin(String userID, Integer port) {

        String myAddress = null; // get my IP address
        try {
            myAddress = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e) {
            Main.logger.severe("Unable to retrieve my IP address to report to address server.");
            return;
        }
        String urlToRead = servUrl + "/check-in/" + userID + "/" + myAddress + "/" + port.toString();
        httpGetRequest(urlToRead); // send a GET request to this URL

    }

    public static InetSocketAddress lookup(String userID) {

        String urlToRead = servUrl + "/lookup/" + userID;
        String response = httpGetRequest(urlToRead);

        if(response.equals("None")) // userID not present in the book
            return null;
        else { // address is here
            String[] tokens = response.split(":");
            InetSocketAddress addr = new InetSocketAddress(tokens[0], Integer.parseInt(tokens[1])); // error handling?
            return addr;
        }

    }

    public static boolean contains(String userID) {
        InetSocketAddress addr = lookup(userID);
        return (addr != null);
    }

    public static HashMap<String, InetSocketAddress> getAll() {

        HashMap<String, InetSocketAddress> table = new HashMap<>();

        String urlToRead = servUrl + "/display";
        String response = httpGetRequest(urlToRead);

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
