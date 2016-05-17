package scaffolding;

import exception.MessengerOfflineException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
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

    public static String httpGetRequest(String urlToRead) throws MessengerOfflineException {

            // if unsuccessful – returns null

            String result = null;
            try {


                StringBuilder sb = new StringBuilder();


                URL url = new URL(urlToRead);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000); // read timeout at 3 sec
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line + "\n");
                }
                rd.close();

                result = sb.toString().trim(); // response by the server


            }
            catch (IOException e) {
                System.err.print("Unable to do a HTTP GET request to the server...");
                //e.printStackTrace();
                throw new MessengerOfflineException(); // something went wrong... (mb offline?)
            }

            return result;

    }

    public static String httpPostRequest(String targetUrl, String msg) throws MessengerOfflineException {

        // if unsuccessful – returns null

        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(targetUrl);

        RequestConfig rc = RequestConfig.custom().setConnectTimeout(2000).build(); // connection times out after 2 sec
        post.setConfig(rc);

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
        } catch (IOException e) {
            //System.err.print("Unable to do a HTTP POST request to the server...");
            //e.printStackTrace();
            throw new MessengerOfflineException(); // something went wrong... (mb offline?)
        }


        return result.toString();

    }

}
