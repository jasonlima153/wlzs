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

public class UdpServerActivity extends AppCompatActivity {

    private EditText etPort, etMessage, etTargetIp, etTargetPort;
    private SwitchMaterial switchHex;
    private MaterialButton btnStart, btnStop, btnSend, btnClear;
    private LogAdapter logAdapter;
    private DatagramSocket socket;
    private volatile boolean running = false;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udp_server);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("UDP 服务端");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etPort = findViewById(R.id.et_port);
        etMessage = findViewById(R.id.et_message);
        etTargetIp = findViewById(R.id.et_target_ip);
        etTargetPort = findViewById(R.id.et_target_port);
        switchHex = findViewById(R.id.switch_hex);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnSend = findViewById(R.id.btn_send);
        btnClear = findViewById(R.id.btn_clear);
        RecyclerView recyclerLog = findViewById(R.id.recycler_log);

        recyclerLog.setLayoutManager(new LinearLayoutManager(this));
        logAdapter = new LogAdapter(this);
        recyclerLog.setAdapter(logAdapter);

        btnStart.setOnClickListener(v -> startServer());
        btnStop.setOnClickListener(v -> stopServer());
        btnSend.setOnClickListener(v -> sendReply());
        btnClear.setOnClickListener(v -> logAdapter.clear());
    }

    private void startServer() {
        int port;
        try {
            port = Integer.parseInt(etPort.getText().toString().trim());
        } catch (NumberFormatException e) {
            logAdapter.addEntry(new LogEntry(LogEntry.TYPE_ERROR, "端口号无效"));
            return;
        }

        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        logAdapter.addEntry(new LogEntry(LogEntry.TYPE_INFO, "UDP 服务端启动中，监听端口 " + port + "…"));

        executor.execute(() -> {
            try {
                socket = new DatagramSocket(port);
                running = true;
                handler.post(() -> {
                    logAdapter.addEntry(new LogEntry(LogEntry.TYPE_RECEIVE, "UDP 服务端已启动，监听端口 " + port));
                    btnSend.setEnabled(true);
                    autoScrollLog();
                });

                byte[] buffer = new byte[4096];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                while (running) {
                    try {
                        socket.receive(packet);
                        String data = new String(packet.getData(), 0, packet.getLength());
                        String addr = packet.getAddress().getHostAddress() + ":" + packet.getPort();
                        handler.post(() -> {
                            logAdapter.addEntry(new LogEntry(LogEntry.TYPE_RECEIVE, "[" + addr + "] " + data));
                            autoScrollLog();
                        });
                        // Reset packet length for next receive
                        packet.setLength(buffer.length);
                    } catch (Exception e) {
                        if (running) {
                            handler.post(() -> {
                                logAdapter.addEntry(new LogEntry(LogEntry.TYPE_ERROR, "接收错误: " + e.getMessage()));
                                autoScrollLog();
                            });
                        }
                    }
                }
            } catch (Exception e) {
                handler.post(() -> {
                    logAdapter.addEntry(new LogEntry(LogEntry.TYPE_ERROR, "启动失败: " + e.getMessage()));
                    btnStart.setEnabled(true);
                    autoScrollLog();
                });
            }
        });
    }

    private void sendReply() {
        String message = etMessage.getText().toString();
        if (message.isEmpty()) return;

        String targetIp = etTargetIp.getText().toString().trim();
        int targetPort;
        try {
            targetPort = Integer.parseInt(etTargetPort.getText().toString().trim());
        } catch (NumberFormatException e) {
            logAdapter.addEntry(new LogEntry(LogEntry.TYPE_ERROR, "目标端口无效"));
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

        executor.execute(() -> {
            try {
                InetAddress addr = InetAddress.getByName(targetIp);
                DatagramPacket packet = new DatagramPacket(data, data.length, addr, targetPort);
                if (socket != null) socket.send(packet);
                handler.post(() -> {
                    logAdapter.addEntry(new LogEntry(LogEntry.TYPE_SEND, "[发送到 " + targetIp + ":" + targetPort + "] " + message));
                    autoScrollLog();
                });
            } catch (Exception e) {
                handler.post(() -> {
                    logAdapter.addEntry(new LogEntry(LogEntry.TYPE_ERROR, "发送失败: " + e.getMessage()));
                    autoScrollLog();
                });
            }
        });
    }

    private void stopServer() {
        running = false;
        if (socket != null) socket.close();
        logAdapter.addEntry(new LogEntry(LogEntry.TYPE_INFO, "UDP 服务端已停止"));
        btnStart.setEnabled(true);
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
        stopServer();
        executor.shutdownNow();
        super.onDestroy();
    }
}
