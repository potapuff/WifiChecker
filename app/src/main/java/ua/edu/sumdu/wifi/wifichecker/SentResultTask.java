package ua.edu.sumdu.wifi.wifichecker;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.iid.InstanceID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by User on 15.01.2018.
 */

public class SentResultTask extends AsyncTask<Void, Integer, Void> {

    private final String TAG = "wifichecker:SEND";

    @SuppressLint("StaticFieldLeak")
    private ScrollingActivity mContext;


    SentResultTask(ScrollingActivity context) {
        mContext = context;
    }

    private JSONObject collectResults() {
        JSONObject main = new JSONObject();
        try {
            JSONArray wifis = new JSONArray();
            for (HashMap<String, String> wifi : mContext.WifiList) {
                JSONObject current = new JSONObject();
                for (String key : wifi.keySet()) {
                    current.put(key, wifi.get(key));
                }
                wifis.put(current);
            }
            main.put("networks", wifis);
            main.put("startDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
            main.put("uuid", UUID.randomUUID().toString());
            String devId = InstanceID.getInstance(mContext).getId();;
            main.put("instance", devId);
        } catch (JSONException e) {
            Log.e("JSON SEND", e.getMessage());
            Log.e("JSON SEND", e.getStackTrace().toString());
        }
        if (main.length() > 0) {
            return main;
        } else {
            Toast.makeText(mContext, "Fail to generate result's set", Toast.LENGTH_SHORT).show();// display toast
        }
        return null;
    }


    @Override
    protected Void doInBackground(Void... params) {
        try {
            JSONObject results = collectResults();
            URL url = new URL("https://dl.sumdu.edu.ua/wifi");
            Log.d(TAG, "Send results");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            OutputStream os = conn.getOutputStream();
            os.write(results.toString().getBytes("UTF-8"));
            os.close();
            conn.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void onPostExecute(String result) {
        Log.i(TAG, "Connection test onPostExecute");
        Toast.makeText( mContext, "Result is sended", Toast.LENGTH_SHORT).show();
    }
}
