package ua.edu.sumdu.wifi.wifichecker;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

//TODO implement performance  best practices (caching, static tag etc.)
public class WifiAdapter extends ArrayAdapter<HashMap<String, String>> {

    static final String NO_INTERNET = "No internet";
    static final String SSID = "SSID";
    static final String LEVEL = "level";
    static final String CAPABILITIES = "capabilities";
    static final String STATUS = "status";
    static final String SECURITY = "security";
    static final String SECURITY_OPEN = "open";
    static final String PENDING = "pending...";
    static final String DEBUG = "debug";
    static final String[] SECURITY_TYPE_LIST = {"WPA", "WPA", "WEP", "IEEE8021X"};
    static final String STATE_CHECKING = "connection testing...";

    WifiAdapter(Context context,
                ArrayList<HashMap<String, String>> wifiList) {
        super(context, 0 , wifiList);
    }

    @Override
    public int getCount() {
        return super.getCount();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        HashMap<String, String> wifiData = getItem(position);
        if (wifiData == null){
            wifiData = new HashMap<>();
        }
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.wifiitem, parent, false);
        }
        TextView ssid = (TextView) convertView.findViewById(R.id.ssid);
        TextView status = (TextView) convertView.findViewById(R.id.status);

        ImageView complete = (ImageView) convertView.findViewById(R.id.strength);

        String ssid_value = wifiData.get(WifiAdapter.SSID);
        ssid.setText(ssid_value  != null ? ssid_value : "Empty");
        status.setText(wifiData.get(WifiAdapter.STATUS));

        ProgressBar progress = (ProgressBar) convertView.findViewById(R.id.progress);
        //TODO fix progressbar  - it does'n work
        //TODO - we able use show real progress from Connection Test task
        if (WifiAdapter.STATUS.equals(WifiAdapter.STATE_CHECKING)) {
            progress.setVisibility(View.VISIBLE);
        }
        else {
            progress.setVisibility(View.GONE);
        }

        if (wifiData.get(WifiAdapter.SECURITY).equals(WifiAdapter.SECURITY_OPEN)){
            int level = Integer.parseInt(wifiData.get(WifiAdapter.LEVEL));
            if (level <= -70) {
                complete.setImageResource(R.drawable.no_wifi);
            } else if (level <= -60) {
                complete.setImageResource(R.drawable.quat_wifi);
            } else if (level <= -40) {
                complete.setImageResource(R.drawable.half_wifi);
            } else if (level <= -30) {
                complete.setImageResource(R.drawable.full_wifi);
            }
        } else{
            complete.setImageResource(R.drawable.lock);
        }

        return convertView;

    }

}