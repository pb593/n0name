import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by pb593 on 28/01/2016.
 */
public class HTTPHandler {

    public static String httpGetRequest(String urlToRead) {

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

    public static String httpPostRequest(String targetUrl, String msg) {

    }

}
