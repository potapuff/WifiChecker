package ua.edu.sumdu.wifi.wifichecker;

import android.annotation.SuppressLint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;


import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.WIFI_SERVICE;
import static android.os.SystemClock.sleep;

public class ConnectionTask extends AsyncTask<String, Integer, String> {

    private final String TAG = "wifichecker:TASK";

    @SuppressLint("StaticFieldLeak")
    private ScrollingActivity mContext;

    // https://stackoverflow.com/questions/9570237/android-check-internet-connection
    private Pair<Integer,String> isInternetAvailable() {
        Log.i(TAG, "Detecting an internet");
        String random = Long.toString(System.currentTimeMillis());
        Pair<Integer,String> response = HttpHelper.post(mContext.getString(R.string.ping_url)+random, null);
        return response;
    }

    ConnectionTask(ScrollingActivity context)
    {
        mContext = context;
    }

    @Override
    protected String doInBackground(String... params) {
        String ssid = params[0];
        Log.i(TAG, "Start checking task for "+ssid);
        try {
            mContext.setStatus(ssid, WifiAdapter.STATE_CHECKING, "");
            WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(WIFI_SERVICE);
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = String.format("\"%s\"", ssid);
            wifiConfig.preSharedKey = String.format("\"%s\"", "");

            wifiManager.setWifiEnabled(true);
            Log.i(TAG, "Try connect to " + ssid);
            int netId = wifiManager.addNetwork(wifiConfig);
            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            wifiManager.reconnect();

            ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(CONNECTIVITY_SERVICE);
            int tries = 20;
            NetworkInfo wifi;
            while ((wifi = connManager.getActiveNetworkInfo()) == null && tries > 0) {
                Log.d(TAG, "wait for connection init ...");
                tries -= 1;
                sleep(500);
            }
            //Wait while connecting...
            if (wifi != null) {
                while (wifi.isConnectedOrConnecting() && !wifi.isConnected() && tries > 0) {
                    Log.d(TAG, "wait for connection ...");
                    tries -= 1;
                    sleep(500);
                }
            }
            Log.i(TAG,"  "+wifi.getExtraInfo() + "\n   " +
                    wifi.getState().toString() + "\n   " +
                    wifi.getSubtypeName() + "\n   " +
                    wifi.getDetailedState().toString() + "\n   ");
            if (wifi != null && wifi.isConnected()) {
                Pair<Integer,String> pair =  isInternetAvailable();
                mContext.setStatus(ssid,  pair.first == 200 ? "connected" : "internet failure", pair.second);
            } else {
                mContext.setStatus(ssid, "connection failure", wifi.getDetailedState().toString());
            }
        } catch (NullPointerException  ex){
            mContext.setStatus(ssid, "device error", Util.exceptionToString(ex));
            Log.e(TAG, ex.getMessage());
            Log.e(TAG, ex.toString());
        }
        return "OK";
    }

    @Override
    protected void onPostExecute(String result) {
       Log.i(TAG, "Connection test onPostExecute: "+((result == null) ? "fail" : result));
       mContext.wifiAdapter.notifyDataSetChanged();
    }


}
