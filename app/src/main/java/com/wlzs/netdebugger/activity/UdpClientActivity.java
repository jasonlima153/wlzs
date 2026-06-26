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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UdpClientActivity extends AppCompatActivity {

    private EditText etHost, etPort, etMessage;
    private SwitchMaterial switchHex;
    private MaterialButton btnSend, btnClear;
    private LogAdapter logAdapter;
    private DatagramSocket socket;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udp_client);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("UDP 客户端");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etHost = findViewById(R.id.et_host);
        etPort = findViewById(R.id.et_port);
        etMessage = findViewById(R.id.et_message);
        switchHex = findViewById(R.id.switch_hex);
        btnSend = findViewById(R.id.btn_send);
        btnClear = findViewById(R.id.btn_clear);
        RecyclerView recyclerLog = findViewById(R.id.recycler_log);

        recyclerLog.setLayoutManager(new LinearLayoutManager(this));
        logAdapter = new LogAdapter(this);
        recyclerLog.setAdapter(logAdapter);

        btnSend.setOnClickListener(v -> sendMessage());
        btnClear.setOnClickListener(v -> logAdapter.clear());

        // Create socket for receiving
        executor.execute(() -> {
            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(3000);
                byte[] buffer = new byte[4096];
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        socket.receive(receivePacket);
                        String data = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        String addr = receivePacket.getAddress().getHostAddress() + ":" + receivePacket.getPort();
                        handler.post(() -> {
                            logAdapter.addEntry(new LogEntry(LogEntry.TYPE_RECEIVE, "[" + addr + "] " + data));
                            autoScrollLog();
                        });
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                handler.post(() -> logAdapter.addEntry(new LogEntry(LogEntry.TYPE_ERROR, "UDP Socket 创建失败: " + e.getMessage())));
            }
        });
    }

    private void sendMessage() {
        String host = etHost.getText().toString().trim();
        String message = etMessage.getText().toString();
        if (host.isEmpty() || message.isEmpty()) return;

        int port;
        try {
            port = Integer.parseInt(etPort.getText().toString().trim());
        } catch (NumberFormatException e) {
            logAdapter.addEntry(new LogEntry(LogEntry.TYPE_ERROR, "端口号无效"));
            return;
        }

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

        logAdapter.addEntry(new LogEntry(LogEntry.TYPE_SEND, "[发送到 " + host + ":" + port + "] " + message));
        autoScrollLog();

        executor.execute(() -> {
            try {
                InetAddress addr = InetAddress.getByName(host);
                DatagramPacket packet = new DatagramPacket(data, data.length, addr, port);
                if (socket != null) socket.send(packet);
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
        if (socket != null) socket.close();
        executor.shutdownNow();
        super.onDestroy();
    }
}
