package ua.edu.sumdu.wifi.wifichecker;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class WifiAdapter extends ArrayAdapter<HashMap<String, String>> {

    static String SSID = "SSID";
    static String LEVEL = "level";
    static String CAPABILITIES = "capabilities";
    static String STATUS = "status";
    static String SECURITY = "security";
    static String SECURITY_OPEN = "open";
    static String PENDING = "pending...";
    static String[] SECURITY_TYPE_LIST = {"WPA", "WPA", "WEP", "IEEE8021X"};

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

        HashMap<String, String> Wifidata = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.wifiitem, parent, false);
        }
        TextView ssid = (TextView) convertView.findViewById(R.id.ssid);
        TextView tvLevel = (TextView) convertView.findViewById(R.id.level);
        TextView capability = (TextView) convertView.findViewById(R.id.capabilities);
        TextView bssid = (TextView) convertView.findViewById(R.id.bssid);

        ImageView complete = (ImageView) convertView.findViewById(R.id.strength);

        String ssid_value = Wifidata.get(WifiAdapter.SSID);
        ssid.setText(ssid_value  != null ? ssid_value : "Empty");
        String level_value = Wifidata.get(WifiAdapter.LEVEL);
        tvLevel.setText("Wifi level " + (level_value != null ? level_value : "-1"));
        capability.setText("capabilities " + Wifidata.get(WifiAdapter.CAPABILITIES));
        bssid.setText(Wifidata.get(WifiAdapter.STATUS));


        int level = Integer.parseInt(Wifidata.get(WifiAdapter.LEVEL));
        if (level <= -70) {
            complete.setImageResource(R.drawable.ic_no_wifi_white);
        } else if (level <= -60) {
            complete.setImageResource(R.drawable.ic_quat_wifi_white);
        } else if (level <= -40) {
            complete.setImageResource(R.drawable.ic_half_wifi_white);
        } else if (level <= -30) {
            complete.setImageResource(R.drawable.ic_full_wifi_white);
        }


        return convertView;

    }

}