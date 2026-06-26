package com.wlzs.netdebugger.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.wlzs.netdebugger.R;
import com.wlzs.netdebugger.util.IpLocationUtil;
import com.wlzs.netdebugger.util.NetworkUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IpLocationActivity extends AppCompatActivity {

    private EditText etIp;
    private TextView tvResult;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ip_location);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("IP 归属地");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etIp = findViewById(R.id.et_ip);
        tvResult = findViewById(R.id.tv_result);
        MaterialButton btnQuery = findViewById(R.id.btn_query);

        // Fill in current IP
        String currentIp = NetworkUtils.getLocalIpAddress();
        if (currentIp != null && !currentIp.isEmpty()) {
            etIp.setText(currentIp);
        }

        btnQuery.setOnClickListener(v -> queryLocation());
    }

    private void queryLocation() {
        String ip = etIp.getText().toString().trim();
        if (ip.isEmpty()) return;

        tvResult.setText("查询中…");

        executor.execute(() -> {
            String result = IpLocationUtil.lookup(this, ip);
            handler.post(() -> tvResult.setText(result));
        });
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
