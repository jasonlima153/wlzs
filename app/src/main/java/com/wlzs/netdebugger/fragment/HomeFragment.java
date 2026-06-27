package com.wlzs.netdebugger.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.wlzs.netdebugger.R;
import com.wlzs.netdebugger.activity.IpLocationActivity;
import com.wlzs.netdebugger.activity.LanScanActivity;
import com.wlzs.netdebugger.activity.WifiInfoActivity;
import com.wlzs.netdebugger.util.NetworkUtils;
import com.wlzs.netdebugger.util.WifiUtils;

import java.util.List;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            // WiFi info
            setInfoRow(view, R.id.wifi_ssid, "SSID", safeGet(() -> WifiUtils.getSSID(requireContext())));
            setInfoRow(view, R.id.wifi_ip, "IP", safeGet(() -> WifiUtils.getIpAddress(requireContext())));
            setInfoRow(view, R.id.wifi_gateway, "网关", safeGet(() -> WifiUtils.getGateway(requireContext())));
            String dns = "N/A";
            try {
                List<String> dnsServers = WifiUtils.getDnsServers(requireContext());
                dns = dnsServers.isEmpty() ? "N/A" : String.join(", ", dnsServers);
            } catch (Exception e) { /* permission denied */ }
            setInfoRow(view, R.id.wifi_dns, "DNS", dns);
            setInfoRow(view, R.id.wifi_mac, "MAC", NetworkUtils.getMacAddress());
            setInfoRow(view, R.id.wifi_signal, "信号", safeGet(() -> WifiUtils.getSignalLevel(requireContext())));
            String freq = "N/A";
            try {
                int f = WifiUtils.getFrequency(requireContext());
                if (f > 0) freq = f + " MHz";
            } catch (Exception e) { /* ignore */ }
            setInfoRow(view, R.id.wifi_frequency, "频率", freq);

            // Mobile info
            setInfoRow(view, R.id.mobile_type, "网络类型", safeGet(() -> NetworkUtils.getNetworkType(requireContext())));
            setInfoRow(view, R.id.mobile_ip, "移动IP", NetworkUtils.getLocalIpAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Buttons
        try {
            view.findViewById(R.id.btn_wifi_detail).setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), WifiInfoActivity.class));
            });

            view.findViewById(R.id.btn_ip_location).setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), IpLocationActivity.class));
            });

            view.findViewById(R.id.btn_lan_scan).setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), LanScanActivity.class));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String safeGet(RunnableWithResult supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return "N/A";
        }
    }

    private void setInfoRow(View parent, int id, String label, String value) {
        try {
            View row = parent.findViewById(id);
            if (row == null) return;
            TextView tvLabel = row.findViewById(R.id.tv_info_label);
            TextView tvValue = row.findViewById(R.id.tv_info_value);
            if (tvLabel != null) tvLabel.setText(label);
            if (tvValue != null) tvValue.setText(value != null ? value : "N/A");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private interface RunnableWithResult {
        String get();
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            if (getView() != null) {
                setInfoRow(getView(), R.id.wifi_ssid, "SSID", safeGet(() -> WifiUtils.getSSID(requireContext())));
                setInfoRow(getView(), R.id.wifi_ip, "IP", safeGet(() -> WifiUtils.getIpAddress(requireContext())));
                setInfoRow(getView(), R.id.wifi_gateway, "网关", safeGet(() -> WifiUtils.getGateway(requireContext())));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
