import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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

        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(targetUrl);

        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add(new BasicNameValuePair("msg", msg));

        StringBuffer result = new StringBuffer();
        try {
            post.setEntity(new UrlEncodedFormEntity(urlParameters));

            HttpResponse response = client.execute(post);

            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));

            String line = null;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
        } catch (Exception e) {
            Main.logger.severe("Unable to do a HTTP POST request to the address server...");
            e.printStackTrace();
            System.exit(-1);
        }


        return result.toString();

    }

}
