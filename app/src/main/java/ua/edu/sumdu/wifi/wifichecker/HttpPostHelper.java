package ua.edu.sumdu.wifi.wifichecker;

import android.net.Network;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;


class HttpPostHelper {

    private static String TAG = "wifichecker:HttpHelper";

    private HttpURLConnection getConnection(URL url) throws IOException {
        HttpURLConnection conn;
        if (network == null) {
             conn = (HttpURLConnection) url.openConnection();
        } else {
             conn = (HttpURLConnection) network.openConnection(url);
        }
        conn.setConnectTimeout(5000);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        Log.d(TAG, "Connection created");
        return conn;
    }

    private Integer responseCode  = 500;
    private String responseBody = null;
    private Network network = null;

    HttpPostHelper(String address, String playload){
        this(null, address, playload);
    }

    HttpPostHelper(Network network, String address, String playload) {
        this.network = network;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(address);
            conn = getConnection(url);
            OutputStream os = conn.getOutputStream();
            if (playload != null) {
                os.write(playload.getBytes("UTF-8"));
            }
            os.close();

            Log.d(TAG, "Submit:" + playload);
            // get reponce body
            BufferedReader reader;
            StringBuilder stringBuilder;

            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            stringBuilder = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            responseCode = conn.getResponseCode();
            responseBody = stringBuilder.toString();

            Log.d(TAG, "SERVER ANSWER:" + conn.getResponseMessage());
            Log.d(TAG, url.toString());
            Log.d(TAG, responseBody);

        } catch (UnknownHostException ex) {
            responseBody = WifiAdapter.NO_INTERNET;
        } catch (Exception e) {
            e.printStackTrace();
            responseBody =  Util.exceptionToString(e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    Integer getCode(){
       return responseCode == null ? 500 : responseCode;
    }

    String getBody(){
        return responseBody == null ? "" : responseBody;
    }
    boolean isSuccessful(){
        return getCode() == 200;
    }
}
