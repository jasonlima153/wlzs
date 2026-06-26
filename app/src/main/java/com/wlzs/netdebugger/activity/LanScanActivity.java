package com.wlzs.netdebugger.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.wlzs.netdebugger.R;
import com.wlzs.netdebugger.adapter.LanDeviceAdapter;
import com.wlzs.netdebugger.model.LanDevice;
import com.wlzs.netdebugger.util.NetworkUtils;
import com.wlzs.netdebugger.util.WifiUtils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class LanScanActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView tvStatus;
    private LanDeviceAdapter adapter;
    private final List<LanDevice> devices = new ArrayList<>();
    private volatile boolean scanning = false;
    private final ExecutorService executor = Executors.newFixedThreadPool(50);
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lan_scan);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("局域网扫描");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        progressBar = findViewById(R.id.progress_bar);
        tvStatus = findViewById(R.id.tv_status);
        MaterialButton btnScan = findViewById(R.id.btn_scan);
        RecyclerView recyclerDevices = findViewById(R.id.recycler_devices);

        recyclerDevices.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LanDeviceAdapter(this, devices);
        recyclerDevices.setAdapter(adapter);

        btnScan.setOnClickListener(v -> startScan());
    }

    private void startScan() {
        String subnet = WifiUtils.getSubnetAddress(this);
        if (subnet.isEmpty()) {
            tvStatus.setText("无法获取网段地址，请确保WiFi已连接");
            return;
        }

        devices.clear();
        adapter.notifyDataSetChanged();
        scanning = true;
        progressBar.setVisibility(ProgressBar.VISIBLE);
        progressBar.setMax(254);
        progressBar.setProgress(0);

        tvStatus.setText("正在扫描 " + subnet + "0/24 …");
        final AtomicInteger count = new AtomicInteger(0);

        for (int i = 1; i <= 254; i++) {
            final String ip = subnet + i;
            executor.execute(() -> {
                try {
                    InetAddress address = InetAddress.getByName(ip);
                    boolean reachable = address.isReachable(300);
                    if (reachable) {
                        String hostname = address.getHostName();
                        String mac = "N/A";
                        LanDevice device = new LanDevice(ip, mac, hostname, true);
                        synchronized (devices) {
                            devices.add(device);
                        }
                        handler.post(() -> {
                            adapter.notifyDataSetChanged();
                            tvStatus.setText("发现设备: " + devices.size());
                        });
                    }
                } catch (Exception ignored) {}

                int current = count.incrementAndGet();
                handler.post(() -> {
                    progressBar.setProgress(current);
                    tvStatus.setText(String.format("扫描进度: %d/254 | 发现 %d 台设备", current, devices.size()));
                });
            });
        }

        // Mark as finished after all tasks complete
        executor.execute(() -> {
            try {
                Thread.sleep(5000); // Wait for remaining tasks
            } catch (InterruptedException ignored) {}
            handler.post(() -> {
                scanning = false;
                progressBar.setVisibility(ProgressBar.GONE);
                tvStatus.setText("扫描完成! 共发现 " + devices.size() + " 台设备");
            });
        });
    }

    @Override
    protected void onDestroy() {
        scanning = false;
        executor.shutdownNow();
        super.onDestroy();
    }
}
