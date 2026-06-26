package com.wlzs.netdebugger.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wlzs.netdebugger.R;
import com.wlzs.netdebugger.activity.DnsQueryActivity;
import com.wlzs.netdebugger.activity.PingActivity;
import com.wlzs.netdebugger.activity.PortScanActivity;
import com.wlzs.netdebugger.activity.TracerouteActivity;
import com.wlzs.netdebugger.adapter.ToolGridAdapter;
import com.wlzs.netdebugger.model.ToolItem;

import java.util.ArrayList;
import java.util.List;

public class ToolsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tools, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recycler = view.findViewById(R.id.recycler_tools);
        recycler.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        List<ToolItem> items = new ArrayList<>();
        items.add(new ToolItem("Ping", R.drawable.ic_menu_share, "网络连通性测试"));
        items.add(new ToolItem("DNS 查询", R.drawable.ic_menu_search, "查询域名DNS记录"));
        items.add(new ToolItem("端口扫描", R.drawable.ic_menu_set_as, "扫描开放端口"));
        items.add(new ToolItem("Traceroute", R.drawable.ic_menu_directions, "路由追踪"));

        ToolGridAdapter adapter = new ToolGridAdapter(requireContext(), items, (item, position) -> {
            Intent intent = null;
            switch (position) {
                case 0: intent = new Intent(requireContext(), PingActivity.class); break;
                case 1: intent = new Intent(requireContext(), DnsQueryActivity.class); break;
                case 2: intent = new Intent(requireContext(), PortScanActivity.class); break;
                case 3: intent = new Intent(requireContext(), TracerouteActivity.class); break;
            }
            if (intent != null) startActivity(intent);
        });
        recycler.setAdapter(adapter);
    }
}
