package com.wlzs.netdebugger.activity;

import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.wlzs.netdebugger.R;
import com.wlzs.netdebugger.adapter.LogAdapter;
import com.wlzs.netdebugger.model.LogEntry;
import com.wlzs.netdebugger.util.TracerouteUtil;

public class TracerouteActivity extends AppCompatActivity {

    private EditText etHost;
    private MaterialButton btnStart, btnStop;
    private LogAdapter logAdapter;
    private TracerouteUtil tracerouteUtil = new TracerouteUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traceroute);

        try {
            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                getSupportActionBar().setTitle("Traceroute");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                toolbar.setNavigationOnClickListener(v -> onBackPressed());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        etHost = findViewById(R.id.et_host);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        RecyclerView recyclerLog = findViewById(R.id.recycler_log);

        recyclerLog.setLayoutManager(new LinearLayoutManager(this));
        logAdapter = new LogAdapter(this);
        recyclerLog.setAdapter(logAdapter);

        btnStart.setOnClickListener(v -> startTraceroute());
        btnStop.setOnClickListener(v -> stopTraceroute());
    }

    private void startTraceroute() {
        String host = etHost.getText().toString().trim();
        if (host.isEmpty()) return;

        logAdapter.clear();
        logAdapter.addEntry(new LogEntry(LogEntry.TYPE_INFO, "Traceroute to " + host + "…"));
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);

        tracerouteUtil = new TracerouteUtil();
        tracerouteUtil.startTraceroute(host, 30, new TracerouteUtil.TracerouteListener() {
            @Override
            public void onHop(int hop, String address, String latency) {
                String line = String.format("  %2d  %-15s  %s", hop, address, latency);
                logAdapter.addEntry(new LogEntry(
                        "*".equals(address) ? LogEntry.TYPE_WARN : LogEntry.TYPE_INFO, line));
                autoScrollLog();
            }

            @Override
            public void onError(String error) {
                logAdapter.addEntry(new LogEntry(LogEntry.TYPE_ERROR, error));
                autoScrollLog();
            }

            @Override
            public void onFinished() {
                logAdapter.addEntry(new LogEntry(LogEntry.TYPE_INFO, "Traceroute 完成"));
                btnStart.setEnabled(true);
                btnStop.setEnabled(false);
            }
        });
    }

    private void stopTraceroute() {
        tracerouteUtil.stop();
        logAdapter.addEntry(new LogEntry(LogEntry.TYPE_WARN, "用户已停止 Traceroute"));
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
    }

    private void autoScrollLog() {
        RecyclerView recycler = findViewById(R.id.recycler_log);
        recycler.post(() -> recycler.smoothScrollToPosition(logAdapter.getItemCount() - 1));
    }

    @Override
    protected void onDestroy() {
        tracerouteUtil.stop();
        super.onDestroy();
    }
}
