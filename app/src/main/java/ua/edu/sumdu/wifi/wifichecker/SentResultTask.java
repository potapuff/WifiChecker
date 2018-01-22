package ua.edu.sumdu.wifi.wifichecker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.google.android.gms.iid.InstanceID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static android.content.Context.WIFI_SERVICE;

public class SentResultTask extends AsyncTask<Void, String, String> {

    private final String TAG = "wifichecker:SEND";

    @SuppressLint("StaticFieldLeak")
    private ScrollingActivity mContext;


    SentResultTask(ScrollingActivity context) {
        mContext = context;
    }

    @SuppressLint("SimpleDateFormat")
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
            String devId = InstanceID.getInstance(mContext).getId();
            main.put("instance", devId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return (main.length() > 0) ? main : null;
    }

    private void turnOffWifi() {
        //Send result data via moblie internet
        Context app_context = mContext.getApplicationContext();
        WifiManager wifi = (WifiManager) app_context.getSystemService(WIFI_SERVICE);
        if (wifi != null) {
            wifi.setWifiEnabled(false);
        }
        ConnectivityManager connectivityManager =
                (ConnectivityManager) app_context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            connectivityManager.getActiveNetworkInfo();
        }
    }

    private SharedPreferences getPreference() {
        return mContext.getSharedPreferences(
                mContext.getString(R.string.results_holder), Context.MODE_PRIVATE);
    }

    private void putForProcessing(JSONObject entry) {
        //store results before sending (will be re-sanded next time, if send will file)
        if (entry == null) {
            return;
        }
        SharedPreferences sharedPref = getPreference();
        SharedPreferences.Editor editor = sharedPref.edit();
        String key = Long.toString(System.currentTimeMillis());
        String stringEntry = entry.toString();
        editor.putString(key, stringEntry);
        editor.apply();
    }

    @Override
    protected String doInBackground(Void... params) {
        putForProcessing(collectResults());
        String url = mContext.getString(R.string.service_uri);
        turnOffWifi();
        Log.d(TAG, "Send results");
        SharedPreferences preference = getPreference();
        if (preference == null) {
            return null;
        }
        SharedPreferences.Editor editor = preference.edit();
        for (Map.Entry<String, ?> result : preference.getAll().entrySet()) {
            String playload = (String) result.getValue();
            Pair<Integer, String> response = HttpHelper.post(url, playload);
            if (response.first != null && response.first == 200) {
                editor.remove(result.getKey());
            }
        }
        editor.apply();
        return "OK";
    }

    @Override
    protected void onPostExecute(String result) {
        Log.i(TAG, "Connection test onPostExecute:" + ((result == null) ? "fail" : result));
        Toast.makeText(mContext, "Result is submitted", Toast.LENGTH_SHORT).show();
    }
}
