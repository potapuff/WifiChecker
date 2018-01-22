package ua.edu.sumdu.wifi.wifichecker;

import android.util.Log;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

class HttpHelper {

    private static String TAG = "wifichecker:HttpHelper";

    private static HttpURLConnection getConnection(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        Log.d(TAG, "Connection created");
        return conn;
    }

    static Pair<Integer, String> post(String address, String playload) {
        HttpURLConnection conn = null;
        Pair <Integer, String> response;
        try {
            URL url = new URL(address);
            conn = HttpHelper.getConnection(url);
            OutputStream os = conn.getOutputStream();
            if (playload != null) {
                os.write(playload.getBytes("UTF-8"));
            }
            os.close();

            Log.d(TAG, "Submit:" + playload);
            //-- debug -------------------------------------------------------------------------
            BufferedReader reader;
            StringBuilder stringBuilder;

            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            stringBuilder = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            Log.d(TAG, "SERVER ANSWER:" + conn.getResponseMessage());
            Log.d(TAG, url.toString());
            Log.d(TAG, stringBuilder.toString());
            //-- end debug ---------------------------------------------------------------------
            response = new Pair(conn.getResponseCode(), line);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            response = new Pair(500, Util.exceptionToString(e));
        } catch (IOException e) {
            response = new Pair(500, Util.exceptionToString(e));
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return response;
    }
}
