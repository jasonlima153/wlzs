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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpServerActivity extends AppCompatActivity {

    private EditText etPort, etMessage;
    private SwitchMaterial switchHex;
    private MaterialButton btnListen, btnStop, btnSend, btnClear;
    private LogAdapter logAdapter;
    private ServerSocket serverSocket;
    private final List<Socket> clientSockets = new ArrayList<>();
    private volatile boolean listening = false;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tcp_server);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("TCP 服务端");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etPort = findViewById(R.id.et_port);
        etMessage = findViewById(R.id.et_message);
        switchHex = findViewById(R.id.switch_hex);
        btnListen = findViewById(R.id.btn_listen);
        btnStop = findViewById(R.id.btn_stop);
        btnSend = findViewById(R.id.btn_send);
        btnClear = findViewById(R.id.btn_clear);
        RecyclerView recyclerLog = findViewById(R.id.recycler_log);

        recyclerLog.setLayoutManager(new LinearLayoutManager(this));
        logAdapter = new LogAdapter(this);
        recyclerLog.setAdapter(logAdapter);

        btnListen.setOnClickListener(v -> startListening());
        btnStop.setOnClickListener(v -> stopListening());
        btnSend.setOnClickListener(v -> sendMessage());
        btnClear.setOnClickListener(v -> logAdapter.clear());
    }

    private void startListening() {
        int port;
        try {
            port = Integer.parseInt(etPort.getText().toString().trim());
        } catch (NumberFormatException e) {
            logAdapter.addEntry(new LogEntry(LogEntry.TYPE_ERROR, "端口号无效"));
            return;
        }

        logAdapter.addEntry(new LogEntry(LogEntry.TYPE_INFO, "正在监听端口 " + port + "…"));
        btnListen.setEnabled(false);
        btnStop.setEnabled(true);

        executor.execute(() -> {
            try {
                serverSocket = new ServerSocket(port);
                listening = true;
                handler.post(() -> {
                    logAdapter.addEntry(new LogEntry(LogEntry.TYPE_RECEIVE, "服务端已启动，监听端口 " + port));
                    btnSend.setEnabled(true);
                    autoScrollLog();
                });

                while (listening) {
                    try {
                        Socket client = serverSocket.accept();
                        clientSockets.add(client);
                        String clientAddr = client.getRemoteSocketAddress().toString();
                        handler.post(() -> {
                            logAdapter.addEntry(new LogEntry(LogEntry.TYPE_RECEIVE, "客户端已连接: " + clientAddr));
                            autoScrollLog();
                        });

                        // Start reading from this client
                        final Socket c = client;
                        executor.execute(() -> readClient(c));
                    } catch (Exception e) {
                        if (listening) {
                            handler.post(() -> {
                                logAdapter.addEntry(new LogEntry(LogEntry.TYPE_ERROR, "接受连接错误: " + e.getMessage()));
                                autoScrollLog();
                            });
                        }
                    }
                }
            } catch (Exception e) {
                handler.post(() -> {
                    logAdapter.addEntry(new LogEntry(LogEntry.TYPE_ERROR, "启动失败: " + e.getMessage()));
                    btnListen.setEnabled(true);
                    autoScrollLog();
                });
            }
        });
    }

    private void readClient(Socket client) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            char[] buffer = new char[4096];
            int len;
            while (!client.isClosed() && (len = reader.read(buffer)) != -1) {
                String data = new String(buffer, 0, len);
                String addr = client.getRemoteSocketAddress().toString();
                handler.post(() -> {
                    logAdapter.addEntry(new LogEntry(LogEntry.TYPE_RECEIVE, "[" + addr + "] " + data));
                    autoScrollLog();
                });
            }
        } catch (Exception ignored) {}
    }

    private void sendMessage() {
        String message = etMessage.getText().toString();
        if (message.isEmpty() || clientSockets.isEmpty()) return;

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

        logAdapter.addEntry(new LogEntry(LogEntry.TYPE_SEND, "[广播] " + message));
        autoScrollLog();

        executor.execute(() -> {
            for (Socket client : new ArrayList<>(clientSockets)) {
                try {
                    OutputStream os = client.getOutputStream();
                    os.write(data);
                    os.flush();
                } catch (Exception e) {
                    handler.post(() -> {
                        logAdapter.addEntry(new LogEntry(LogEntry.TYPE_ERROR, "发送到客户端失败: " + e.getMessage()));
                        autoScrollLog();
                    });
                }
            }
        });
    }

    private void stopListening() {
        listening = false;
        try {
            if (serverSocket != null) serverSocket.close();
            for (Socket client : clientSockets) {
                try { client.close(); } catch (Exception ignored) {}
            }
            clientSockets.clear();
        } catch (Exception ignored) {}

        logAdapter.addEntry(new LogEntry(LogEntry.TYPE_INFO, "服务端已停止"));
        btnListen.setEnabled(true);
        btnStop.setEnabled(false);
        btnSend.setEnabled(false);
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
        stopListening();
        executor.shutdownNow();
        super.onDestroy();
    }
}
