package ua.edu.sumdu.wifi.wifichecker;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.HashMap;

import techibi.vibwifi_lib.MultipleScan;
import techibi.vibwifi_lib.vibwifi;

//TODO Add icon for application
public class ScrollingActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 120;
    private final String TAG = "wifichecker";

    //TODO (#1) replace ArrayList<HashMap<String, String>> with HashMap<String(ssid),HashMap<String, String>>
    // to increase productivity
    ArrayList<HashMap<String, String>> wifiList;
    WifiAdapter wifiAdapter;
    vibwifi wifiManager;
    private State currentState;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUI(intent);
        }
    };

    /** Notify user about current app status */
    protected void showAppStatus(String status){
        Snackbar.make(findViewById(R.id.content), status, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Fix problems with request in main tread https://stackoverflow.com/questions/22395417/error-strictmodeandroidblockguardpolicy-onnetwork
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        setContentView(R.layout.activity_scrolling);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.reset_button);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disableState(State.ShowReset);
                enableState(State.Scanning);
            }
        });

        wifiList = new ArrayList<>();
        wifiAdapter = new WifiAdapter(ScrollingActivity.this, wifiList);
        ListView userList = (ListView) findViewById(R.id.list);
        userList.setItemsCanFocus(false);
        userList.setAdapter(wifiAdapter);

        disableState(State.ShowReset);
        enableState(State.Scanning);
    }

    /** update wifi status - find wifi  by ssid  in list and update status
     * Amount of code can be reduced by implementing TODO #1
    */
    public void setWifiStatus(String ssid, String status, String debug) {
        for (HashMap<String, String> wifi : wifiList) {
            String current_ssid = wifi.get(WifiAdapter.SSID);
            if (current_ssid.equals(ssid)) {
                Log.d(TAG,"Updates "+ ssid+" status to "+status);
                wifi.put(WifiAdapter.STATUS, status);
                wifi.put(WifiAdapter.DEBUG, debug == null ? "" : debug);
            }
        }
    }

    /** Force to update list of wifis in interface
     */
    protected void updateUI() {
        if (!wifiList.isEmpty()) {
            ListView userList = (ListView) findViewById(R.id.list);
            userList.setAdapter(wifiAdapter);
        } else {
            Log.d(TAG, "No wifi yet");
            //TODO: Show something if no wifis
        }

    }


    /**
     * List sacn results and try to find new wifi points
     * Amount of code can be reduced by implementing TODO #1
     * @param new_wifis - list of wifi scan results
     */
    private void addWifis(ArrayList<HashMap<String, String>> new_wifis) {
        int beforeAdd = wifiList.size();
        if (new_wifis == null) {
            return;
        }
        enableState(State.GotResult);
        for (HashMap<String, String> wifi : new_wifis) {
            String ssid = wifi.get(WifiAdapter.SSID);
            boolean isNew = true;
            for (HashMap<String, String> list : wifiList) {
                if (list.get(WifiAdapter.SSID).equals(ssid)) {
                    isNew = false;
                }
            }
            if (isNew) {
                String capabilities = wifi.get(WifiAdapter.CAPABILITIES);
                for (String capability : WifiAdapter.SECURITY_TYPE_LIST) {
                    if (capabilities.contains(capability)) {
                        wifi.put(WifiAdapter.SECURITY, capability);
                    }
                }
                if (!wifi.containsKey(WifiAdapter.SECURITY)) {
                    wifi.put(WifiAdapter.SECURITY, WifiAdapter.SECURITY_OPEN);
                }
                wifi.put(WifiAdapter.STATUS, WifiAdapter.PENDING);
                wifiList.add(wifi);
            }
        }
        int afterAdd = wifiList.size();
        Log.d(TAG, "List size:"+Integer.toString(beforeAdd)+"--->"+Integer.toString(afterAdd));
        //If no new results....
        if (beforeAdd > 0 &&  afterAdd == beforeAdd){
            Log.d(TAG, "Stop scaning!");
            disableState(State.Scanning);
            enableState(State.Processing);
        }
    }

    private void updateUI(Intent intent) {
        Log.d(TAG, "serialized data.." + wifiList);
        addWifis((ArrayList<HashMap<String, String>>) intent.getSerializableExtra("wifilist"));
        updateUI();
    }

    private void runConnectionCheck() {
        // TODO: Replace with task executor with with priority
        //       then disable network scanning only on sending scanning results
        Log.d(TAG, "run Connection Test");
        Log.d(TAG, "Found: "+Integer.toString(wifiList.size())+" networks");
        for (HashMap<String, String> wifi : wifiList) {
            if (!wifi.get(WifiAdapter.STATUS).equals(WifiAdapter.PENDING)){
                continue;
            }
            String ssid = wifi.get(WifiAdapter.SSID);
            String security = wifi.get(WifiAdapter.SECURITY);
            Log.d(TAG, "Check:" + ssid);
            if (security.equals(WifiAdapter.SECURITY_OPEN)) {
                Log.d(TAG, "Schedule to investigate " + ssid);
                new ConnectionTask(ScrollingActivity.this).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ssid);
            } else {
                Log.d(TAG, "Skip wifi with " + security);
                setWifiStatus(ssid, "require password","");
            }
        }
        // Send results after all wifi is checked
        Log.d(TAG, "Schedule to send results ");
        new SentResultTask(ScrollingActivity.this).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    @Override
    public void onPause() {
        super.onPause();
        disableState(State.Scanning);
        disableState(State.Processing);
    }

    @Override
    public void onResume() {
        super.onResume();
        enableState(State.Scanning);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disableState(State.Scanning);
    }

    public void performScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);

        } else {
            if (wifiManager == null) {
                wifiManager = new vibwifi();
                wifiManager.StartScan(ScrollingActivity.this, 500);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            performScan();

        }
    }

    /**
     *  Enable new application state (act like a state-machine)
     * @param newState
     */
    void enableState(State newState) {
        Log.i(TAG, "Set state: "+ (currentState == null ? "?" : currentState.toString()) + "->" +newState.toString());
        if (this.currentState == newState){
            return;
        }
        switch (newState) {
            case Scanning:
                showAppStatus("Поиск сетей ...");
                wifiList.clear();
                registerReceiver(broadcastReceiver, new IntentFilter(MultipleScan.BROADCAST_ACTION));
                performScan();
                break;
            case GotResult:
                break;
            case Processing:
                showAppStatus("Проверка сетей ...");
                runConnectionCheck();
                break;
            case ShowReset:
                showAppStatus("Информация отправлена ...");
                findViewById(R.id.reset_button).setVisibility(View.VISIBLE);
                break;
        }
        this.currentState = newState;
    }

    /** Roll-back state */
    private void disableState(State current){
        Log.i(TAG, "Disable state: "+current.toString());
        switch (current){
            case Scanning:
                if (wifiManager != null) {
                    wifiManager.StopScan(ScrollingActivity.this);
                    wifiManager = null;
                }
                try {
                    unregisterReceiver(broadcastReceiver);
                } catch (IllegalArgumentException ex) {
                    Log.e(TAG, ex.getMessage());
                }
                break;
            case GotResult:
                break;
            case Processing:
                break;
            case ShowReset:
                findViewById(R.id.reset_button).setVisibility(View.INVISIBLE);
                wifiList.clear();
                updateUI();
                break;
        }
        if (this.currentState == current){
            this.currentState = null;
        }
    }

    enum State  {
        Scanning,   // Initials state - start scan wifi's
        GotResult,  // Got first results
        Processing, // No new networks - let check points status and then - send results
        ShowReset   // Show button for reset to initial state
    }

}
