package com.wlzs.netdebugger.activity;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.wlzs.netdebugger.R;
import com.wlzs.netdebugger.adapter.LogAdapter;
import com.wlzs.netdebugger.model.LogEntry;
import com.wlzs.netdebugger.util.PingUtil;

public class PingActivity extends AppCompatActivity {

    private EditText etHost, etCount;
    private SwitchMaterial switchContinuous;
    private MaterialButton btnStart, btnStop;
    private TextView tvStats;
    private LogAdapter logAdapter;
    private PingUtil pingUtil = new PingUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ping);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Ping");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etHost = findViewById(R.id.et_host);
        etCount = findViewById(R.id.et_count);
        switchContinuous = findViewById(R.id.switch_continuous);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        tvStats = findViewById(R.id.tv_stats);
        RecyclerView recyclerLog = findViewById(R.id.recycler_log);

        recyclerLog.setLayoutManager(new LinearLayoutManager(this));
        logAdapter = new LogAdapter(this);
        recyclerLog.setAdapter(logAdapter);

        btnStart.setOnClickListener(v -> startPing());
        btnStop.setOnClickListener(v -> stopPing());

        switchContinuous.setOnCheckedChangeListener((buttonView, isChecked) -> {
            etCount.setEnabled(!isChecked);
        });
    }

    private void startPing() {
        String host = etHost.getText().toString().trim();
        if (host.isEmpty()) return;

        int count = 4;
        if (!switchContinuous.isChecked()) {
            try {
                count = Integer.parseInt(etCount.getText().toString().trim());
            } catch (NumberFormatException e) {
                count = 4;
            }
        }

        logAdapter.clear();
        tvStats.setText("状态: 运行中…");
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);

        pingUtil = new PingUtil();
        pingUtil.startPing(host, count, switchContinuous.isChecked(), new PingUtil.PingListener() {
            @Override
            public void onLine(String line) {
                logAdapter.addEntry(new LogEntry(LogEntry.TYPE_INFO, line));
                autoScrollLog();
            }

            @Override
            public void onStats(String stats) {
                tvStats.setText(stats);
            }

            @Override
            public void onError(String error) {
                logAdapter.addEntry(new LogEntry(LogEntry.TYPE_ERROR, error));
                autoScrollLog();
            }

            @Override
            public void onFinished() {
                tvStats.setText(tvStats.getText() + "\n状态: 已完成");
                btnStart.setEnabled(true);
                btnStop.setEnabled(false);
            }
        });
    }

    private void stopPing() {
        pingUtil.stopPing();
        logAdapter.addEntry(new LogEntry(LogEntry.TYPE_WARN, "用户已停止 Ping"));
        tvStats.setText(tvStats.getText() + "\n状态: 已停止");
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
    }

    private void autoScrollLog() {
        RecyclerView recycler = findViewById(R.id.recycler_log);
        recycler.post(() -> recycler.smoothScrollToPosition(logAdapter.getItemCount() - 1));
    }

    @Override
    protected void onDestroy() {
        pingUtil.stopPing();
        super.onDestroy();
    }
}
