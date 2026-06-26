package com.wlzs.netdebugger.activity;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.wlzs.netdebugger.R;
import com.wlzs.netdebugger.adapter.LogAdapter;
import com.wlzs.netdebugger.model.LogEntry;
import com.wlzs.netdebugger.util.PortScanUtil;

import java.util.List;

public class PortScanActivity extends AppCompatActivity {

    private EditText etHost, etStartPort, etEndPort;
    private MaterialButton btnScan, btnStop;
    private ProgressBar progressBar;
    private TextView tvProgress;
    private LogAdapter logAdapter;
    private PortScanUtil portScanUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_port_scan);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("端口扫描");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etHost = findViewById(R.id.et_host);
        etStartPort = findViewById(R.id.et_start_port);
        etEndPort = findViewById(R.id.et_end_port);
        btnScan = findViewById(R.id.btn_scan);
        btnStop = findViewById(R.id.btn_stop);
        progressBar = findViewById(R.id.progress_bar);
        tvProgress = findViewById(R.id.tv_progress);
        RecyclerView recyclerResults = findViewById(R.id.recycler_results);

        recyclerResults.setLayoutManager(new LinearLayoutManager(this));
        logAdapter = new LogAdapter(this);
        recyclerResults.setAdapter(logAdapter);

        btnScan.setOnClickListener(v -> startScan());
        btnStop.setOnClickListener(v -> stopScan());
    }

    private void startScan() {
        String host = etHost.getText().toString().trim();
        if (host.isEmpty()) return;

        int startPort = 1, endPort = 1024;
        try {
            startPort = Integer.parseInt(etStartPort.getText().toString().trim());
        } catch (NumberFormatException ignored) {}
        try {
            endPort = Integer.parseInt(etEndPort.getText().toString().trim());
        } catch (NumberFormatException ignored) {}

        if (endPort < startPort) endPort = startPort;

        logAdapter.clear();
        logAdapter.addEntry(new LogEntry(LogEntry.TYPE_INFO, "开始扫描 " + host + " 端口 " + startPort + "-" + endPort));
        progressBar.setVisibility(ProgressBar.VISIBLE);
        tvProgress.setVisibility(TextView.VISIBLE);
        progressBar.setMax(endPort - startPort + 1);
        progressBar.setProgress(0);
        btnScan.setEnabled(false);
        btnStop.setEnabled(true);

        portScanUtil = new PortScanUtil();
        portScanUtil.startScan(host, startPort, endPort, 500, new PortScanUtil.ScanListener() {
            @Override
            public void onPortFound(int port, String service) {
                logAdapter.addEntry(new LogEntry(LogEntry.TYPE_RECEIVE, "端口 " + port + " 开放 [" + service + "]"));
            }

            @Override
            public void onProgress(int current, int total) {
                progressBar.setProgress(current);
                tvProgress.setText(String.format("进度: %d / %d", current, total));
            }

            @Override
            public void onFinished(List<PortScanUtil.PortResult> results) {
                logAdapter.addEntry(new LogEntry(LogEntry.TYPE_INFO, "扫描完成! 共发现 " + results.size() + " 个开放端口"));
                progressBar.setVisibility(ProgressBar.GONE);
                tvProgress.setVisibility(TextView.GONE);
                btnScan.setEnabled(true);
                btnStop.setEnabled(false);
            }

            @Override
            public void onError(String error) {
                logAdapter.addEntry(new LogEntry(LogEntry.TYPE_ERROR, error));
                progressBar.setVisibility(ProgressBar.GONE);
                btnScan.setEnabled(true);
                btnStop.setEnabled(false);
            }
        });
    }

    private void stopScan() {
        if (portScanUtil != null) portScanUtil.stopScan();
        logAdapter.addEntry(new LogEntry(LogEntry.TYPE_WARN, "用户已停止扫描"));
        progressBar.setVisibility(ProgressBar.GONE);
        tvProgress.setVisibility(TextView.GONE);
        btnScan.setEnabled(true);
        btnStop.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        if (portScanUtil != null) portScanUtil.stopScan();
        super.onDestroy();
    }
}
