package ua.edu.sumdu.wifi.wifichecker;

import android.annotation.SuppressLint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.WIFI_SERVICE;


public class ConnectionTask extends AsyncTask<String, Integer, String> {

    private final String TAG = "wifichecker:TASK";

    @SuppressLint("StaticFieldLeak")
    private ScrollingActivity mContext;

    // https://stackoverflow.com/questions/9570237/android-check-internet-connection
    private boolean isInternetAvailable() {
        Log.i(TAG, "Detecting an internet");
        try {
            final InetAddress address = InetAddress.getByName("www.google.com");
            Log.i(TAG, "adress: "+address.toString());
            return !address.equals("");
        } catch (UnknownHostException e) {
            /* Log error */
        }
        return false;
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
            mContext.setStatus(ssid, "connection testing...");
            WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(WIFI_SERVICE);
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = String.format("\"%s\"", ssid);
            wifiConfig.preSharedKey = String.format("\"%s\"", "");

            wifiManager.setWifiEnabled(true);
            Log.i(TAG, "Try connect to "+ssid);
            int netId = wifiManager.addNetwork(wifiConfig);
            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            wifiManager.reconnect();

            ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo wifi = connManager.getActiveNetworkInfo();
            Log.i(TAG,"  "+wifi.getExtraInfo() + "\n   " +
            wifi.getState().toString() + "\n   " +
            wifi.getSubtypeName() + "\n   " +
            wifi.getDetailedState().toString() + "\n   ");
            if (wifi.isConnectedOrConnecting()) {
                mContext.setStatus(ssid, isInternetAvailable() ? "connected" : "internet failure");
            } else {
                mContext.setStatus(ssid, "connection failure");
            }
        } catch (NullPointerException  ex){
            mContext.setStatus(ssid, "device error");
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
