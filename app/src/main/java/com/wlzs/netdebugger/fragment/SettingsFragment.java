package com.wlzs.netdebugger.fragment;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.button.MaterialButton;
import com.wlzs.netdebugger.BuildConfig;
import com.wlzs.netdebugger.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {

    private SharedPreferences prefs;
    private RecyclerView recyclerConfigs;
    private TextView tvEmptyHint;
    private ConfigAdapter adapter;
    private List<ConfigItem> configList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            prefs = requireActivity().getSharedPreferences("settings", 0);

            // 快捷配置管理
            recyclerConfigs = view.findViewById(R.id.recycler_configs);
            tvEmptyHint = view.findViewById(R.id.tv_empty_hint);
            recyclerConfigs.setLayoutManager(new LinearLayoutManager(requireContext()));

            adapter = new ConfigAdapter();
            recyclerConfigs.setAdapter(adapter);

            MaterialButton btnAdd = view.findViewById(R.id.btn_add_config);
            btnAdd.setOnClickListener(v -> showAddConfigDialog());

            // 加载已保存的配置
            loadConfigs();

            // 深色模式
            SwitchMaterial switchDarkMode = view.findViewById(R.id.switch_dark_mode);
            boolean isDark = prefs.getBoolean("dark_mode", false);
            switchDarkMode.setChecked(isDark);
            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("dark_mode", isChecked).apply();
                AppCompatDelegate.setDefaultNightMode(
                        isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            });

            // 加载默认设置
            EditText etPingHost = view.findViewById(R.id.et_default_ping_host);
            etPingHost.setText(prefs.getString("default_ping_host", "8.8.8.8"));

            EditText etPingCount = view.findViewById(R.id.et_default_ping_count);
            etPingCount.setText(prefs.getString("default_ping_count", "0"));

            EditText etDnsServer = view.findViewById(R.id.et_default_dns_server);
            etDnsServer.setText(prefs.getString("default_dns_server", "8.8.8.8"));

            EditText etPortStart = view.findViewById(R.id.et_default_port_start);
            etPortStart.setText(prefs.getString("default_port_start", "1"));

            EditText etPortEnd = view.findViewById(R.id.et_default_port_end);
            etPortEnd.setText(prefs.getString("default_port_end", "1024"));

            // 关于
            TextView tvAbout = view.findViewById(R.id.tv_about_summary);
            tvAbout.setText(getString(R.string.settings_about_summary, BuildConfig.VERSION_NAME));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // 保存默认设置
        if (prefs != null && getView() != null) {
            try {
                SharedPreferences.Editor editor = prefs.edit();
                EditText etPingHost = getView().findViewById(R.id.et_default_ping_host);
                if (etPingHost != null) editor.putString("default_ping_host", etPingHost.getText().toString());

                EditText etPingCount = getView().findViewById(R.id.et_default_ping_count);
                if (etPingCount != null) editor.putString("default_ping_count", etPingCount.getText().toString());

                EditText etDnsServer = getView().findViewById(R.id.et_default_dns_server);
                if (etDnsServer != null) editor.putString("default_dns_server", etDnsServer.getText().toString());

                EditText etPortStart = getView().findViewById(R.id.et_default_port_start);
                if (etPortStart != null) editor.putString("default_port_start", etPortStart.getText().toString());

                EditText etPortEnd = getView().findViewById(R.id.et_default_port_end);
                if (etPortEnd != null) editor.putString("default_port_end", etPortEnd.getText().toString());
                editor.apply();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ========== 配置管理 ==========

    private void loadConfigs() {
        configList.clear();
        try {
            String json = prefs.getString("saved_configs", "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                ConfigItem item = new ConfigItem();
                item.id = obj.optLong("id", System.currentTimeMillis());
                item.name = obj.optString("name", "配置");
                item.type = obj.optString("type", "ping");
                item.target = obj.optString("target", "");
                item.detail = obj.optString("detail", "");
                configList.add(item);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        adapter.notifyDataSetChanged();
        if (tvEmptyHint != null) {
            tvEmptyHint.setVisibility(configList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void saveConfigs() {
        try {
            JSONArray arr = new JSONArray();
            for (ConfigItem item : configList) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.id);
                obj.put("name", item.name);
                obj.put("type", item.type);
                obj.put("target", item.target);
                obj.put("detail", item.detail);
                arr.put(obj);
            }
            prefs.edit().putString("saved_configs", arr.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void showAddConfigDialog() {
        String[] types = {"Ping", "DNS 查询", "端口扫描", "Traceroute", "TCP 连接", "UDP 发送", "HTTP 请求"};
        new AlertDialog.Builder(requireContext())
                .setTitle("选择配置类型")
                .setItems(types, (dialog, which) -> {
                    String type = types[which];
                    String typeKey = type.toLowerCase().split(" ")[0].replace("查询", "dns").replace("连接", "tcp").replace("发送", "udp").replace("请求", "http");
                    showConfigEditor(-1, type, typeKey);
                })
                .show();
    }

    private void showConfigEditor(int position, String typeName, String typeKey) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("添加" + typeName + "配置");

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.item_config_dialog, null);
        EditText etName = dialogView.findViewById(R.id.et_config_name);
        EditText etTarget = dialogView.findViewById(R.id.et_config_target);
        EditText etExtra = dialogView.findViewById(R.id.et_config_extra);

        String hintText = "目标地址";
        String extraHint = "额外参数";
        String extraLabel = "参数";

        switch (typeKey) {
            case "ping":
                hintText = "目标地址 (如 8.8.8.8)";
                extraHint = "Ping次数 (0=持续)";
                extraLabel = "次数";
                break;
            case "dns":
                hintText = "域名 (如 baidu.com)";
                extraHint = "DNS服务器 (如 8.8.8.8)";
                extraLabel = "DNS服务器";
                break;
            case "端口扫描":
            case "端口":
                hintText = "目标IP (如 192.168.1.1)";
                extraHint = "端口范围 (如 1-1024)";
                extraLabel = "端口";
                break;
            case "traceroute":
                hintText = "目标地址 (如 8.8.8.8)";
                extraHint = "最大跳数";
                extraLabel = "跳数";
                break;
            case "tcp":
                hintText = "地址:端口 (如 192.168.1.1:8080)";
                extraHint = "发送数据";
                extraLabel = "数据";
                break;
            case "udp":
                hintText = "地址:端口 (如 8.8.8.8:53)";
                extraHint = "发送数据";
                extraLabel = "数据";
                break;
            case "http":
                hintText = "URL (如 http://example.com)";
                extraHint = "请求方法 (GET/POST)";
                extraLabel = "方法";
                break;
        }

        etTarget.setHint(hintText);
        etExtra.setHint(extraHint);

        TextView tvExtraLabel = dialogView.findViewById(R.id.tv_config_extra_label);
        tvExtraLabel.setText(extraLabel);

        if (position >= 0 && position < configList.size()) {
            ConfigItem item = configList.get(position);
            etName.setText(item.name);
            etTarget.setText(item.target);
            etExtra.setText(item.detail);
            builder.setTitle("编辑配置");
        }

        builder.setView(dialogView);

        if (position >= 0) {
            builder.setNegativeButton("删除", (dialog, which) -> {
                configList.remove(position);
                saveConfigs();
                loadConfigs();
            });
        }

        builder.setPositiveButton("保存", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String target = etTarget.getText().toString().trim();
            String extra = etExtra.getText().toString().trim();

            if (name.isEmpty()) name = typeName + "配置";
            if (target.isEmpty()) return;

            ConfigItem item = new ConfigItem();
            item.id = System.currentTimeMillis();
            item.name = name;
            item.type = typeKey;
            item.target = target;
            item.detail = extra;

            if (position >= 0 && position < configList.size()) {
                configList.set(position, item);
            } else {
                configList.add(item);
            }
            saveConfigs();
            loadConfigs();
        });

        builder.setNegativeButton(position >= 0 ? "删除" : "取消", null);
        builder.setCancelable(true);
        builder.show();
    }

    // ========== 配置适配器 ==========

    static class ConfigItem {
        long id;
        String name;
        String type;
        String target;
        String detail;
    }

    class ConfigAdapter extends RecyclerView.Adapter<ConfigAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_config, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ConfigItem item = configList.get(position);
            holder.tvName.setText(item.name);
            holder.tvDetail.setText(item.type.toUpperCase() + ": " + item.target);
            holder.btnEdit.setOnClickListener(v -> {
                String[] types = {"Ping", "DNS 查询", "端口扫描", "Traceroute", "TCP 连接", "UDP 发送", "HTTP 请求"};
                String[] typeKeys = {"ping", "dns", "端口扫描", "traceroute", "tcp", "udp", "http"};
                int idx = 0;
                for (int i = 0; i < typeKeys.length; i++) {
                    if (typeKeys[i].equals(item.type)) { idx = i; break; }
                }
                showConfigEditor(position, types[idx], typeKeys[idx]);
            });
            holder.btnDelete.setOnClickListener(v -> {
                configList.remove(position);
                saveConfigs();
                loadConfigs();
            });
        }

        @Override
        public int getItemCount() {
            return configList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            TextView tvDetail;
            View btnEdit;
            View btnDelete;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_config_name);
                tvDetail = itemView.findViewById(R.id.tv_config_detail);
                btnEdit = itemView.findViewById(R.id.btn_edit_config);
                btnDelete = itemView.findViewById(R.id.btn_delete_config);
            }
        }
    }
}
