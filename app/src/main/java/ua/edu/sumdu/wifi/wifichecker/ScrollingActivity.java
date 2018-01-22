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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import techibi.vibwifi_lib.MultipleScan;
import techibi.vibwifi_lib.vibwifi;


public class ScrollingActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 120;
    private final String TAG = "wifichecker";
    ArrayList<HashMap<String, String>> WifiList;
    WifiAdapter wifiAdapter;
    vibwifi wifiManager;
    boolean doAnalyze = false; //wait until all sacn results recived

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Fix problems with request in main tread https://stackoverflow.com/questions/22395417/error-strictmodeandroidblockguardpolicy-onnetwork
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        setContentView(R.layout.activity_scrolling);

        WifiList = new ArrayList<>();

        wifiAdapter = new WifiAdapter(ScrollingActivity.this, WifiList);
        ListView userList = (ListView) findViewById(R.id.list);
        userList.setItemsCanFocus(false);
        userList.setAdapter(wifiAdapter);
        setApplicationStatus("Wifi investigation");
        performScan();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setApplicationStatus(null);
            updateUI(intent);
            runConnectionCheck();
        }
    };

    public void setStatus(String ssid, String status, String debug) {
        for (HashMap<String, String> wifi : WifiList) {
            String current_ssid = wifi.get(WifiAdapter.SSID);
            if (current_ssid.equals(ssid)) {
                Log.i(TAG,"updates "+ ssid+" status to "+status);
                wifi.put(WifiAdapter.STATUS, status);
                wifi.put(WifiAdapter.DEBUG, debug == null ? "" : debug);
            }
        }
    }

    protected void updateUI() {
        if (!WifiList.isEmpty()) {
            ListView userList = (ListView) findViewById(R.id.list);
            userList.setAdapter(wifiAdapter);
        } else {
            Toast.makeText(ScrollingActivity.this, "No Wifi Found", Toast.LENGTH_SHORT).show();// display toast
        }

    }

        private void addWifis(ArrayList<HashMap<String, String>> new_wifis) {
        int beforeAdd = WifiList.size();
        if (new_wifis == null) {
            return;
        }
        for (HashMap<String, String> wifi : new_wifis) {
            String ssid = wifi.get(WifiAdapter.SSID);
            boolean isNew = true;
            for (HashMap<String, String> list : WifiList) {
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
                WifiList.add(wifi);
            }
        }
        int afterAdd = WifiList.size();
        if (beforeAdd > 0 && afterAdd == beforeAdd){
            // No new networks, so stop scan
            wifiManager.StopScan(ScrollingActivity.this);
            try {
                unregisterReceiver(broadcastReceiver);
            } catch (IllegalArgumentException ex) {
                Log.e(TAG, ex.getMessage());
            }
            doAnalyze = true;
        }
    }

    private void updateUI(Intent intent) {
        addWifis((ArrayList<HashMap<String, String>>) intent.getSerializableExtra("wifilist"));
        Log.i(TAG, "serialized data.." + WifiList);
        updateUI();
    }

    private void runConnectionCheck() {
        if (!doAnalyze) {
            return;
        }
        Log.i(TAG, "run Connection fToastCheck");
        for (HashMap<String, String> wifi : WifiList) {
            if (!wifi.get(WifiAdapter.STATUS).equals(WifiAdapter.PENDING)){
                continue;
            }
            String ssid = wifi.get(WifiAdapter.SSID);
            String security = wifi.get(WifiAdapter.SECURITY);
            Log.i(TAG, "Check:" + ssid);
            if (security.equals(WifiAdapter.SECURITY_OPEN)) {
                new ConnectionTask(ScrollingActivity.this).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ssid);
            } else {
                setStatus(ssid, "require password","");
                Log.i(TAG, "Skip wifi with " + security);
            }
        }
        new SentResultTask(ScrollingActivity.this).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //WifiList.clear();
        registerReceiver(broadcastReceiver, new IntentFilter(MultipleScan.BROADCAST_ACTION));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    public void performScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);

        } else {
            if (wifiManager == null) {
                wifiManager = new vibwifi();
                wifiManager.StartScan(ScrollingActivity.this, 500); //3 seconds
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

    private void setApplicationStatus(String status){
        TextView textView = (TextView)  findViewById(R.id.status);
        boolean isEmpty = status == null || status.equals("");
        textView.setVisibility( isEmpty ? View.INVISIBLE : View.VISIBLE);
        if (isEmpty){
            textView.setText(status);
        }
    }

}
