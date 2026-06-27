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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpClientActivity extends AppCompatActivity {

    private static final String CONN_TYPE = "tcp_client";

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

    // Saved connections
    private ConnectionManager connManager;
    private Spinner spinnerSaved;
    private MaterialButton btnSaveConn, btnDelConn;
    private ArrayAdapter<ConnectionManager.ConnectionItem> spinnerAdapter;

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

        initSavedConnections();
    }

    // ======================== Saved Connections ========================

    private void initSavedConnections() {
        try {
            connManager = ConnectionManager.getInstance(this);
            spinnerSaved = findViewById(R.id.spinner_saved);
            btnSaveConn = findViewById(R.id.btn_save_conn);
            btnDelConn = findViewById(R.id.btn_del_conn);

            // Add a placeholder item as prompt
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
                            etHost.setText(item.host);
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
            String currentHost = etHost.getText() != null ? etHost.getText().toString().trim() : "";
            String currentPort = etPort.getText() != null ? etPort.getText().toString().trim() : "";
            if (currentHost.isEmpty() || currentPort.isEmpty()) return;

            TextInputEditText etName = new TextInputEditText(this);
            etName.setHint("配置名称");
            etName.setText(currentHost + ":" + currentPort);

            new AlertDialog.Builder(this)
                    .setTitle("保存连接配置")
                    .setView(etName)
                    .setPositiveButton("保存", (dialog, which) -> {
                        try {
                            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                            if (name.isEmpty()) name = currentHost + ":" + currentPort;
                            ConnectionManager.ConnectionItem item = new ConnectionManager.ConnectionItem();
                            item.name = name;
                            item.host = currentHost;
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

    // ======================== Connection Logic ========================

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
