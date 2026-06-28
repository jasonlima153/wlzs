package com.wlzs.netdebugger.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.wlzs.netdebugger.R;
import com.wlzs.netdebugger.adapter.ConnectionConfigAdapter;
import com.wlzs.netdebugger.util.ConnectionManager;

import java.util.ArrayList;
import java.util.List;

public class ConnectionListActivity extends AppCompatActivity {

    public static final String EXTRA_TOOL_TYPE = "tool_type";
    public static final String EXTRA_TOOL_NAME = "tool_name";

    private static final String[] TOOL_TYPES = {
            "ping", "tcp_client", "tcp_server", "udp_client", "udp_server", "http"
    };

    private String toolType;
    private String toolName;
    private ConnectionManager connManager;
    private ConnectionConfigAdapter adapter;
    private List<ConnectionManager.ConnectionItem> configList;
    private RecyclerView recyclerConfigs;
    private View emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection_list);

        toolType = getIntent().getStringExtra(EXTRA_TOOL_TYPE);
        toolName = getIntent().getStringExtra(EXTRA_TOOL_NAME);

        if (toolType == null || toolName == null) {
            finish();
            return;
        }

        try {
            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                getSupportActionBar().setTitle(toolName);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                toolbar.setNavigationOnClickListener(v -> onBackPressed());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        connManager = ConnectionManager.getInstance(this);
        configList = new ArrayList<>();

        recyclerConfigs = findViewById(R.id.recycler_configs);
        emptyState = findViewById(R.id.empty_state);

        recyclerConfigs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConnectionConfigAdapter(this, configList, new ConnectionConfigAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ConnectionManager.ConnectionItem item, int position) {
                openDebugActivity(item);
            }

            @Override
            public void onDeleteClick(ConnectionManager.ConnectionItem item, int position) {
                showDeleteConfirmDialog(item);
            }
        });
        recyclerConfigs.setAdapter(adapter);

        findViewById(R.id.fab_add).setOnClickListener(v -> showAddConfigDialog());

        loadConfigs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConfigs();
    }

    private void loadConfigs() {
        configList.clear();
        configList.addAll(connManager.getConnections(toolType));
        adapter.notifyDataSetChanged();

        if (configList.isEmpty()) {
            recyclerConfigs.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerConfigs.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    private void showAddConfigDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加" + toolName + "配置");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(48, 24, 48, 0);

        // Config name
        EditText etName = new EditText(this);
        etName.setHint("配置名称（可选）");
        container.addView(etName);

        // Host / URL
        String hostLabel;
        if ("ping".equals(toolType)) {
            hostLabel = "目标地址（如 8.8.8.8）";
        } else if ("http".equals(toolType)) {
            hostLabel = "URL（如 http://example.com）";
        } else {
            hostLabel = "目标地址（如 192.168.1.1）";
        }

        EditText etHost = new EditText(this);
        etHost.setHint(hostLabel);
        etHost.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        LinearLayout.LayoutParams lpHost = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpHost.topMargin = 24;
        etHost.setLayoutParams(lpHost);
        container.addView(etHost);

        // Port (not for ping/http)
        EditText etPort = null;
        if (!"ping".equals(toolType) && !"http".equals(toolType)) {
            etPort = new EditText(this);
            etPort.setHint("端口号");
            etPort.setInputType(InputType.TYPE_CLASS_NUMBER);
            LinearLayout.LayoutParams lpPort = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lpPort.topMargin = 24;
            etPort.setLayoutParams(lpPort);
            container.addView(etPort);
        }

        builder.setView(container);

        builder.setPositiveButton("保存", (dialog, which) -> {
            try {
                String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                String host = etHost.getText() != null ? etHost.getText().toString().trim() : "";
                String port = etPort != null && etPort.getText() != null ? etPort.getText().toString().trim() : "";

                if (host.isEmpty()) {
                    Toast.makeText(ConnectionListActivity.this, "请输入地址", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (name.isEmpty()) {
                    if ("ping".equals(toolType)) {
                        name = "Ping " + host;
                    } else if ("http".equals(toolType)) {
                        name = host.length() > 30 ? host.substring(0, 30) : host;
                    } else {
                        name = host + ":" + port;
                    }
                }

                ConnectionManager.ConnectionItem item = new ConnectionManager.ConnectionItem();
                item.name = name;
                item.host = host;
                item.port = port;
                item.type = toolType;
                connManager.addConnection(item);
                loadConfigs();
                Toast.makeText(ConnectionListActivity.this, "已保存", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showDeleteConfirmDialog(ConnectionManager.ConnectionItem item) {
        new AlertDialog.Builder(this)
                .setTitle("删除配置")
                .setMessage("确定要删除配置 \"" + item.name + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    connManager.deleteConnection(item.id);
                    loadConfigs();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void openDebugActivity(ConnectionManager.ConnectionItem item) {
        Intent intent = null;

        switch (toolType) {
            case "ping":
                intent = new Intent(this, PingActivity.class);
                intent.putExtra("conn_host", item.host);
                break;
            case "tcp_client":
                intent = new Intent(this, TcpClientActivity.class);
                intent.putExtra("conn_host", item.host);
                intent.putExtra("conn_port", item.port);
                break;
            case "tcp_server":
                intent = new Intent(this, TcpServerActivity.class);
                intent.putExtra("conn_port", item.port);
                break;
            case "udp_client":
                intent = new Intent(this, UdpClientActivity.class);
                intent.putExtra("conn_host", item.host);
                intent.putExtra("conn_port", item.port);
                break;
            case "udp_server":
                intent = new Intent(this, UdpServerActivity.class);
                intent.putExtra("conn_port", item.port);
                break;
            case "http":
                intent = new Intent(this, HttpCaptureActivity.class);
                intent.putExtra("conn_host", item.host);
                break;
        }

        if (intent != null) {
            intent.putExtra("conn_name", item.name);
            startActivity(intent);
        }
    }
}
