package com.wlzs.netdebugger.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.wlzs.netdebugger.R;
import com.wlzs.netdebugger.adapter.LogAdapter;
import com.wlzs.netdebugger.model.LogEntry;
import com.wlzs.netdebugger.util.ConnectionManager;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UdpServerActivity extends AppCompatActivity {

    private static final String CONN_TYPE = "udp_server";

    private EditText etPort, etMessage, etTargetIp, etTargetPort;
    private SwitchMaterial switchHex;
    private MaterialButton btnStart, btnStop, btnSend, btnClear;
    private LogAdapter logAdapter;
    private DatagramSocket socket;
    private volatile boolean running = false;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Saved connections
    private ConnectionManager connManager;
    private Spinner spinnerSaved;
    private MaterialButton btnSaveConn, btnDelConn;
    private ArrayAdapter<ConnectionManager.ConnectionItem> spinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udp_server);

        try {
            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                getSupportActionBar().setTitle("UDP 服务端");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                toolbar.setNavigationOnClickListener(v -> onBackPressed());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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

        initSavedConnections();
    }

    // ======================== Saved Connections ========================

    private void initSavedConnections() {
        try {
            connManager = ConnectionManager.getInstance(this);
            spinnerSaved = findViewById(R.id.spinner_saved);
            btnSaveConn = findViewById(R.id.btn_save_conn);
            btnDelConn = findViewById(R.id.btn_del_conn);

            List<ConnectionManager.ConnectionItem> items = connManager.getConnections(CONN_TYPE);
            spinnerAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, items);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerSaved.setAdapter(spinnerAdapter);

            spinnerSaved.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                    if (position >= 0 && position < spinnerAdapter.getCount()) {
                        ConnectionManager.ConnectionItem item = spinnerAdapter.getItem(position);
                        if (item != null) {
                            etPort.setText(item.port);
                        }
                    }
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                }
            });

            btnSaveConn.setOnClickListener(v -> showSaveDialog());
            btnDelConn.setOnClickListener(v -> deleteSelectedConnection());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSaveDialog() {
        try {
            String currentPort = etPort.getText() != null ? etPort.getText().toString().trim() : "";
            if (currentPort.isEmpty()) return;

            TextInputEditText etName = new TextInputEditText(this);
            etName.setHint("配置名称");
            etName.setText("监听:" + currentPort);

            new AlertDialog.Builder(this)
                    .setTitle("保存连接配置")
                    .setView(etName)
                    .setPositiveButton("保存", (dialog, which) -> {
                        try {
                            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                            if (name.isEmpty()) name = "监听:" + currentPort;
                            ConnectionManager.ConnectionItem item = new ConnectionManager.ConnectionItem();
                            item.name = name;
                            item.host = "";
                            item.port = currentPort;
                            item.type = CONN_TYPE;
                            connManager.addConnection(item);
                            refreshSpinner();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteSelectedConnection() {
        try {
            int position = spinnerSaved.getSelectedItemPosition();
            if (position >= 0 && position < spinnerAdapter.getCount()) {
                ConnectionManager.ConnectionItem item = spinnerAdapter.getItem(position);
                if (item != null) {
                    connManager.deleteConnection(item.id);
                    refreshSpinner();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshSpinner() {
        try {
            List<ConnectionManager.ConnectionItem> items = connManager.getConnections(CONN_TYPE);
            spinnerAdapter.clear();
            spinnerAdapter.addAll(items);
            spinnerAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ======================== Server Logic ========================

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
