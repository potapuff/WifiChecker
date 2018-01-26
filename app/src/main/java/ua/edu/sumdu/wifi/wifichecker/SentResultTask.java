package ua.edu.sumdu.wifi.wifichecker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.os.AsyncTask;
import android.util.Log;
import com.google.android.gms.iid.InstanceID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static android.os.SystemClock.sleep;

public class SentResultTask extends AsyncTask<Void, String, String> {

    private final String TAG = "TASK:SEND";

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
            for (HashMap<String, String> wifi : mContext.wifiList) {
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

    /** try to activate network for sending results */
    private void enableNetwork() {
        Context app_context = mContext.getApplicationContext();
        ConnectivityManager connectivityManager =
                (ConnectivityManager) app_context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            int tries = 50;
            while (tries > 0  && (networkInfo == null || networkInfo.getState() == NetworkInfo.State.CONNECTING)) {
                sleep(250);
                tries -= 1;
                networkInfo = connectivityManager.getActiveNetworkInfo();
            }
        }
    }

    /** Storage for results of investigation.
     *  If we have problem with sending resluts - we able send it later
     *  TODO #2 : show number of unsended results in app-toolbar. Add button to sent this results
     */
    private SharedPreferences getPreference() {
        return mContext.getSharedPreferences(
                mContext.getString(R.string.results_holder), Context.MODE_PRIVATE);
    }

    /** Add current result to persisten storage */
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
        Log.i(TAG, " Send results");
        putForProcessing(collectResults());
        String url = mContext.getString(R.string.service_uri);
        enableNetwork();
        SharedPreferences preference = getPreference();
        if (preference == null) {
            return null;
        }
        SharedPreferences.Editor editor = preference.edit();
        for (Map.Entry<String, ?> result : preference.getAll().entrySet()) {
            String playload = (String) result.getValue();
            HttpPostHelper response = new HttpPostHelper(url, playload);
            if (response.isSuccessful()) {
                //Remove from persistent storage only on successful sending
                editor.remove(result.getKey());
            }
        }
        editor.apply();
        return "OK";
    }

    @Override
    protected void onPostExecute(String result) {
        Log.d(TAG, "Connection test onPostExecute:" + ((result == null) ? "fail" : result));
        mContext.enableState(ScrollingActivity.State.ShowReset);
    }
}
