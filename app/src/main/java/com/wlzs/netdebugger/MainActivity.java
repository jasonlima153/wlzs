package com.wlzs.netdebugger;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.wlzs.netdebugger.activity.ConnectionListActivity;
import com.wlzs.netdebugger.adapter.ToolGridAdapter;
import com.wlzs.netdebugger.model.ToolItem;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        RecyclerView recyclerTools = findViewById(R.id.recycler_tools);
        recyclerTools.setLayoutManager(new GridLayoutManager(this, 2));

        List<ToolItem> tools = createToolList();
        ToolGridAdapter adapter = new ToolGridAdapter(this, tools, (item, position) -> {
            Intent intent = new Intent(MainActivity.this, ConnectionListActivity.class);
            intent.putExtra(ConnectionListActivity.EXTRA_TOOL_TYPE, getToolType(position));
            intent.putExtra(ConnectionListActivity.EXTRA_TOOL_NAME, item.getName());
            startActivity(intent);
        });
        recyclerTools.setAdapter(adapter);
    }

    private List<ToolItem> createToolList() {
        List<ToolItem> list = new ArrayList<>();
        list.add(new ToolItem("Ping", R.drawable.ic_ping, "测试网络连通性"));
        list.add(new ToolItem("TCP 客户端", R.drawable.ic_menu_send, "连接远程TCP服务端"));
        list.add(new ToolItem("TCP 服务端", R.drawable.ic_menu_manage, "监听端口等待连接"));
        list.add(new ToolItem("UDP 客户端", R.drawable.ic_menu_upload, "发送UDP数据报"));
        list.add(new ToolItem("UDP 服务端", R.drawable.ic_menu_download, "监听UDP数据报"));
        list.add(new ToolItem("HTTP 抓包", R.drawable.ic_menu_view, "发送HTTP请求查看响应"));
        return list;
    }

    private String getToolType(int position) {
        switch (position) {
            case 0: return "ping";
            case 1: return "tcp_client";
            case 2: return "tcp_server";
            case 3: return "udp_client";
            case 4: return "udp_server";
            case 5: return "http";
            default: return "ping";
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
