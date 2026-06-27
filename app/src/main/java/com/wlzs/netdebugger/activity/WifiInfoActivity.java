package com.wlzs.netdebugger.activity;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.wlzs.netdebugger.R;
import com.wlzs.netdebugger.util.NetworkUtils;
import com.wlzs.netdebugger.util.WifiUtils;

import java.util.List;

public class WifiInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_info);

        try {
            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                getSupportActionBar().setTitle("WiFi 详情");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                toolbar.setNavigationOnClickListener(v -> onBackPressed());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        refreshInfo();
    }

    private void refreshInfo() {
        String ssid = WifiUtils.getSSID(this);
        String bssid = WifiUtils.getBSSID(this);
        String ip = WifiUtils.getIpAddress(this);
        String gateway = WifiUtils.getGateway(this);
        String netmask = WifiUtils.getNetmask(this);
        String mac = NetworkUtils.getMacAddress();
        String signal = WifiUtils.getSignalLevel(this);
        int freq = WifiUtils.getFrequency(this);
        List<String> dnsServers = WifiUtils.getDnsServers(this);

        TextView tvSsid = findViewById(R.id.tv_ssid);
        TextView tvBssid = findViewById(R.id.tv_bssid);
        TextView tvIp = findViewById(R.id.tv_ip);
        TextView tvGateway = findViewById(R.id.tv_gateway);
        TextView tvNetmask = findViewById(R.id.tv_netmask);
        TextView tvDns = findViewById(R.id.tv_dns);
        TextView tvMac = findViewById(R.id.tv_mac);
        TextView tvSignal = findViewById(R.id.tv_signal);
        TextView tvFrequency = findViewById(R.id.tv_frequency);

        tvSsid.setText("SSID: " + (ssid.isEmpty() ? "未连接" : ssid));
        tvBssid.setText("BSSID: " + (bssid != null ? bssid : "N/A"));
        tvIp.setText("IP 地址: " + (ip.isEmpty() ? "N/A" : ip));
        tvGateway.setText("网关: " + (gateway.isEmpty() ? "N/A" : gateway));
        tvNetmask.setText("子网掩码: " + (netmask.isEmpty() ? "N/A" : netmask));
        tvDns.setText("DNS: " + (dnsServers.isEmpty() ? "N/A" : String.join(", ", dnsServers)));
        tvMac.setText("MAC: " + (mac.isEmpty() ? "N/A" : mac));
        tvSignal.setText("信号强度: " + signal);
        tvFrequency.setText("频率: " + (freq > 0 ? freq + " MHz" : "N/A"));
    }
}
