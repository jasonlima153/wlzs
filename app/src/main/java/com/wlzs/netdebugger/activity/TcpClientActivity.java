package com.wlzs.netdebugger.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.wlzs.netdebugger.R;
import com.wlzs.netdebugger.adapter.LogAdapter;
import com.wlzs.netdebugger.model.LogEntry;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpClientActivity extends AppCompatActivity {

    private EditText etHost, etPort, etMessage;
    private SwitchMaterial switchHex;
    private MaterialButton btnConnect, btnDisconnect, btnSend, btnClear;
    private LogAdapter logAdapter;
    private Socket socket;
    private OutputStream outputStream;
    private BufferedReader reader;
    private volatile boolean connected = false;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tcp_client);

        try {
            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                getSupportActionBar().setTitle("TCP 客户端");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                toolbar.setNavigationOnClickListener(v -> onBackPressed());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        etHost = findViewById(R.id.et_host);
        etPort = findViewById(R.id.et_port);
        etMessage = findViewById(R.id.et_message);
        switchHex = findViewById(R.id.switch_hex);
        btnConnect = findViewById(R.id.btn_connect);
        btnDisconnect = findViewById(R.id.btn_disconnect);
        btnSend = findViewById(R.id.btn_send);
        btnClear = findViewById(R.id.btn_clear);
        RecyclerView recyclerLog = findViewById(R.id.recycler_log);

        recyclerLog.setLayoutManager(new LinearLayoutManager(this));
        logAdapter = new LogAdapter(this);
        recyclerLog.setAdapter(logAdapter);

        btnConnect.setOnClickListener(v -> connect());
        btnDisconnect.setOnClickListener(v -> disconnect());
        btnSend.setOnClickListener(v -> sendMessage());
        btnClear.setOnClickListener(v -> logAdapter.clear());
    }

    private void connect() {
        String host = etHost.getText().toString().trim();
        int port;
        try {
            port = Integer.parseInt(etPort.getText().toString().trim());
        } catch (NumberFormatException e) {
            logAdapter.addEntry(new LogEntry(LogEntry.TYPE_ERROR, "端口号无效"));
            return;
        }

        btnConnect.setEnabled(false);
        logAdapter.addEntry(new LogEntry(LogEntry.TYPE_INFO, "正在连接 " + host + ":" + port + "…"));

        executor.execute(() -> {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 5000);
                outputStream = socket.getOutputStream();
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                connected = true;

                handler.post(() -> {
                    logAdapter.addEntry(new LogEntry(LogEntry.TYPE_RECEIVE, "已连接到 " + host + ":" + port));
                    btnDisconnect.setEnabled(true);
                    btnSend.setEnabled(true);
                    autoScrollLog();
                });

                // Read loop
                char[] buffer = new char[4096];
                int len;
                while (connected && (len = reader.read(buffer)) != -1) {
                    String data = new String(buffer, 0, len);
                    handler.post(() -> {
                        logAdapter.addEntry(new LogEntry(LogEntry.TYPE_RECEIVE, "[收到] " + data));
                        autoScrollLog();
                    });
                }
            } catch (Exception e) {
                handler.post(() -> {
                    logAdapter.addEntry(new LogEntry(LogEntry.TYPE_ERROR, "连接失败: " + e.getMessage()));
                    btnConnect.setEnabled(true);
                    autoScrollLog();
                });
            }
        });
    }

    private void disconnect() {
        connected = false;
        try {
            if (socket != null) socket.close();
            if (outputStream != null) outputStream.close();
            if (reader != null) reader.close();
        } catch (Exception ignored) {}

        logAdapter.addEntry(new LogEntry(LogEntry.TYPE_INFO, "已断开连接"));
        btnConnect.setEnabled(true);
        btnDisconnect.setEnabled(false);
        btnSend.setEnabled(false);
    }

    private void sendMessage() {
        String message = etMessage.getText().toString();
        if (message.isEmpty() || !connected) return;

        byte[] data;
        if (switchHex.isChecked()) {
            data = hexStringToBytes(message);
            if (data == null) {
                logAdapter.addEntry(new LogEntry(LogEntry.TYPE_ERROR, "HEX 格式错误"));
                return;
            }
        } else {
            data = message.getBytes();
        }

        logAdapter.addEntry(new LogEntry(LogEntry.TYPE_SEND, "[发送] " + message));
        autoScrollLog();

        executor.execute(() -> {
            try {
                if (outputStream != null) {
                    outputStream.write(data);
                    outputStream.flush();
                }
            } catch (Exception e) {
                handler.post(() -> {
                    logAdapter.addEntry(new LogEntry(LogEntry.TYPE_ERROR, "发送失败: " + e.getMessage()));
                    autoScrollLog();
                });
            }
        });
    }

    private byte[] hexStringToBytes(String hex) {
        hex = hex.replaceAll("\\s+", "");
        if (hex.length() % 2 != 0) return null;
        byte[] bytes = new byte[hex.length() / 2];
        try {
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
            return bytes;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void autoScrollLog() {
        RecyclerView recycler = findViewById(R.id.recycler_log);
        recycler.post(() -> recycler.smoothScrollToPosition(logAdapter.getItemCount() - 1));
    }

    @Override
    protected void onDestroy() {
        disconnect();
        executor.shutdownNow();
        super.onDestroy();
    }
}
