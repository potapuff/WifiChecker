package ua.edu.sumdu.wifi.wifichecker;

import android.annotation.SuppressLint;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import java.util.List;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.WIFI_SERVICE;
import static android.os.SystemClock.sleep;

public class ConnectionTask extends AsyncTask<String, Integer, String> {

    // Some code is adopted from io.particle.android.sdk.utils;
    private final String TAG = "TASK:TEST NETWORK";
    private final int MAX_TRIES = 120; //*0.5 sec wait for connection 

    @SuppressLint("StaticFieldLeak")
    private ScrollingActivity mContext;
    private WifiManager wifiManager;

    ConnectionTask(ScrollingActivity context)
    {
        mContext = context;
    }

    private int getNetworkId(String ssid){
        String quoteSSID = String.format("\"%s\"", ssid);
        int netId = -1;
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        if (list != null) {
            for (WifiConfiguration i : list) {
                if (i.SSID != null && i.SSID.equals(quoteSSID)) {
                    netId = i.networkId;
                    break;
                }
            }
        }
        //Network not found in pre-configured
        if (netId == -1){
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = quoteSSID;
            wifiConfig.preSharedKey = String.format("\"%s\"", "");
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            netId = wifiManager.addNetwork(wifiConfig);
        }
        return netId;
    }

    private void initManager(){
        if (wifiManager == null) {
            wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(WIFI_SERVICE);
        }
        if (wifiManager != null && !wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
    }

    /** Try to find network by network SSID */
    private Pair<Network,NetworkInfo> getNetworkForSSID(String ssid) {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return null;
        }
        String quoteSSID = String.format("\"%s\"", ssid);
        for (Network network : connectivityManager.getAllNetworks()){
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
            if (networkInfo.getExtraInfo().equals(quoteSSID)){
                return new Pair<>(network,networkInfo);
            }
        }
        return null;
    }

    @Override
    protected String doInBackground(String... params) {
        String ssid = params[0];
        String quoteSSID = String.format("\"%s\"", ssid);
        Log.i(TAG, "START TASK: checking  "+ssid);
        try {
            //TODO: move setWifi Status to publish progress;
            mContext.setWifiStatus(ssid, WifiAdapter.STATE_CHECKING, "");
            publishProgress(1);
            initManager();
            int networkId = getNetworkId(ssid);
            if (networkId != -1) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(networkId, true);
                wifiManager.reconnect();
            }

            int tries = MAX_TRIES; //*0.5 = 60 seconds per network
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            Log.d(TAG, "===============================");
            //TODO: добавить нотификацию из цикла with publishProgress(1);
            while (tries > 0  &&
                    ( wifiInfo == null ||
                      !wifiInfo.getSSID().equals(quoteSSID) ||
                      WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState()) == NetworkInfo.DetailedState.CONNECTING ||
                      WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState()) == NetworkInfo.DetailedState.OBTAINING_IPADDR
                    )
                   ){
                wifiInfo = wifiManager.getConnectionInfo();
                Log.d(TAG, wifiInfo.getSSID()+"("+WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState()).toString()+"):"+Integer.toString(tries));
                sleep(500);
                tries -= 1;
            }

            Pair <Network, NetworkInfo > info = getNetworkForSSID(ssid);
            if (info == null ) {
                Log.d(TAG, "Connection falilure (by SSID)");
                //TODO: move statuses strings to resource file
                mContext.setWifiStatus(ssid, "connection failure", "fail to add network");
                return "FAIL";
            }
            Network network = info.first;
            NetworkInfo networkInfo = info.second;

            if (networkInfo.isConnected()) {
                Log.d(TAG, "Detecting an internet");
                String random = Long.toString(System.currentTimeMillis());
                HttpPostHelper request = new HttpPostHelper(network, mContext.getString(R.string.ping_url)+random, null);
                mContext.setWifiStatus(ssid, request.isSuccessful() ? "connected" : "internet failure", request.getBody());
            } else {
                mContext.setWifiStatus(ssid, "connection failure", networkInfo.getDetailedState().toString());
            }

            publishProgress(100);
        } catch (Exception  ex){
            mContext.setWifiStatus(ssid, "device error", Util.exceptionToString(ex));
            Log.e(TAG, ex.getMessage());
            Log.e(TAG, ex.toString());
        }
        return "OK";
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        Log.d(TAG, "Notify to update content in list!");
        mContext.wifiAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPostExecute(String result) {
       Log.d(TAG, "Connection test onPostExecute: "+((result == null) ? "fail" : result));
       mContext.wifiAdapter.notifyDataSetChanged();
    }


}
