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
import com.wlzs.netdebugger.activity.HttpCaptureActivity;
import com.wlzs.netdebugger.activity.TcpClientActivity;
import com.wlzs.netdebugger.activity.TcpServerActivity;
import com.wlzs.netdebugger.activity.UdpClientActivity;
import com.wlzs.netdebugger.activity.UdpServerActivity;
import com.wlzs.netdebugger.adapter.ToolGridAdapter;
import com.wlzs.netdebugger.model.ToolItem;

import java.util.ArrayList;
import java.util.List;

public class CommFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_comm, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recycler = view.findViewById(R.id.recycler_comm);
        recycler.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        List<ToolItem> items = new ArrayList<>();
        items.add(new ToolItem("TCP 客户端", R.drawable.ic_menu_send, "连接远程TCP服务端"));
        items.add(new ToolItem("TCP 服务端", R.drawable.ic_menu_manage, "监听TCP连接"));
        items.add(new ToolItem("UDP 客户端", R.drawable.ic_menu_upload, "发送UDP数据包"));
        items.add(new ToolItem("UDP 服务端", R.drawable.ic_menu_download, "监听UDP数据包"));
        items.add(new ToolItem("HTTP 抓包", R.drawable.ic_menu_view, "发送HTTP请求"));

        ToolGridAdapter adapter = new ToolGridAdapter(requireContext(), items, (item, position) -> {
            Intent intent = null;
            switch (position) {
                case 0: intent = new Intent(requireContext(), TcpClientActivity.class); break;
                case 1: intent = new Intent(requireContext(), TcpServerActivity.class); break;
                case 2: intent = new Intent(requireContext(), UdpClientActivity.class); break;
                case 3: intent = new Intent(requireContext(), UdpServerActivity.class); break;
                case 4: intent = new Intent(requireContext(), HttpCaptureActivity.class); break;
            }
            if (intent != null) startActivity(intent);
        });
        recycler.setAdapter(adapter);
    }
}
