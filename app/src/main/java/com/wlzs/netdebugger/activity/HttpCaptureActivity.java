package com.wlzs.netdebugger.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.wlzs.netdebugger.R;
import com.wlzs.netdebugger.model.LogEntry;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpCaptureActivity extends AppCompatActivity {

    private EditText etUrl, etHeaders, etBody;
    private AutoCompleteTextView actvMethod;
    private TextView tvResponseCode, tvResponseHeaders, tvResponseBody;
    private TextInputLayout tilBody;
    private LinearLayout llBody;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_http_capture);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("HTTP 抓包");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etUrl = findViewById(R.id.et_url);
        etHeaders = findViewById(R.id.et_headers);
        etBody = findViewById(R.id.et_body);
        actvMethod = findViewById(R.id.actv_method);
        tvResponseCode = findViewById(R.id.tv_response_code);
        tvResponseHeaders = findViewById(R.id.tv_response_headers);
        tvResponseBody = findViewById(R.id.tv_response_body);
        tilBody = findViewById(R.id.til_body);
        MaterialButton btnSend = findViewById(R.id.btn_send);

        String[] methods = {"GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"};
        ArrayAdapter<String> methodAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, methods);
        actvMethod.setAdapter(methodAdapter);

        actvMethod.setOnItemClickListener((parent, view, position, id) -> {
            String method = methods[position];
            if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
                tilBody.setVisibility(View.GONE);
            } else {
                tilBody.setVisibility(View.VISIBLE);
            }
        });

        btnSend.setOnClickListener(v -> sendRequest());
    }

    private void sendRequest() {
        String urlStr = etUrl.getText().toString().trim();
        if (urlStr.isEmpty()) return;

        String method = actvMethod.getText().toString().trim().toUpperCase();
        String headersStr = etHeaders.getText().toString().trim();
        String body = etBody.getText().toString().trim();

        tvResponseCode.setText("正在请求…");
        tvResponseHeaders.setText("");
        tvResponseBody.setText("");

        executor.execute(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(method);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);

                // Set headers
                if (!headersStr.isEmpty()) {
                    String[] lines = headersStr.split("\n");
                    for (String line : lines) {
                        int colon = line.indexOf(':');
                        if (colon > 0) {
                            String key = line.substring(0, colon).trim();
                            String value = line.substring(colon + 1).trim();
                            conn.setRequestProperty(key, value);
                        }
                    }
                }

                // POST/PUT body
                if (("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) && !body.isEmpty()) {
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    OutputStream os = conn.getOutputStream();
                    os.write(body.getBytes("UTF-8"));
                    os.flush();
                    os.close();
                }

                int responseCode = conn.getResponseCode();
                StringBuilder headerSb = new StringBuilder();
                for (java.util.Map.Entry<String, java.util.List<String>> entry : conn.getHeaderFields().entrySet()) {
                    if (entry.getKey() != null) {
                        headerSb.append(entry.getKey()).append(": ").append(String.join(", ", entry.getValue())).append("\n");
                    }
                }

                StringBuilder bodySb = new StringBuilder();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        bodySb.append(line).append("\n");
                    }
                    reader.close();
                } catch (Exception e) {
                    // Read error stream
                    try {
                        BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        String line;
                        while ((line = errorReader.readLine()) != null) {
                            bodySb.append(line).append("\n");
                        }
                        errorReader.close();
                    } catch (Exception ignored) {}
                }

                String finalResponseCode = "状态码: " + responseCode + " " + conn.getResponseMessage();
                String finalHeaders = headerSb.toString();
                String finalBody = bodySb.toString();

                runOnUiThread(() -> {
                    tvResponseCode.setText(finalResponseCode);
                    if (responseCode >= 200 && responseCode < 300) {
                        tvResponseCode.setTextColor(getResources().getColor(R.color.success));
                    } else {
                        tvResponseCode.setTextColor(getResources().getColor(R.color.error));
                    }
                    tvResponseHeaders.setText(finalHeaders);
                    tvResponseBody.setText(finalBody);
                });

                conn.disconnect();
            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvResponseCode.setText("请求失败");
                    tvResponseCode.setTextColor(getResources().getColor(R.color.error));
                    tvResponseBody.setText(e.getMessage());
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
