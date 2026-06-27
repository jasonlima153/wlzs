package com.wlzs.netdebugger.util;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 连接配置管理器（单例）
 * 使用 SharedPreferences + JSONArray 持久化存储各类型通信工具的连接配置。
 */
public class ConnectionManager {

    private static final String PREF_NAME = "saved_connections";
    private static final String KEY_DATA = "connections_json";

    private static volatile ConnectionManager instance;

    private final SharedPreferences prefs;

    private ConnectionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 获取单例实例，需要在 Application 或 Activity 中首次调用以提供 Context。
     */
    public static ConnectionManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ConnectionManager.class) {
                if (instance == null) {
                    instance = new ConnectionManager(context);
                }
            }
        }
        return instance;
    }

    // ======================== 数据结构 ========================

    /**
     * 单条连接配置
     */
    public static class ConnectionItem {
        public String id;
        public String name;
        public String host;
        public String port;
        public String type; // tcp_client / tcp_server / udp_client / udp_server / http

        public ConnectionItem() {
            this.id = UUID.randomUUID().toString();
        }

        public ConnectionItem(String id, String name, String host, String port, String type) {
            this.id = id;
            this.name = name;
            this.host = host;
            this.port = port;
            this.type = type;
        }

        @Override
        public String toString() {
            // 用于 Spinner 显示
            return name + "  (" + (type.equals("http") ? host : host + ":" + port) + ")";
        }

        /**
         * 序列化为 JSON 对象
         */
        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("name", name);
            obj.put("host", host);
            obj.put("port", port);
            obj.put("type", type);
            return obj;
        }

        /**
         * 从 JSON 对象反序列化
         */
        public static ConnectionItem fromJson(JSONObject obj) throws JSONException {
            return new ConnectionItem(
                    obj.optString("id", UUID.randomUUID().toString()),
                    obj.optString("name", ""),
                    obj.optString("host", ""),
                    obj.optString("port", ""),
                    obj.optString("type", "")
            );
        }
    }

    // ======================== 核心方法 ========================

    /**
     * 添加一条连接配置
     */
    public void addConnection(ConnectionItem item) {
        if (item == null) return;
        try {
            JSONArray array = readAll();
            array.put(item.toJson());
            writeAll(array);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除一条连接配置（按 id）
     */
    public void deleteConnection(String id) {
        if (id == null || id.isEmpty()) return;
        try {
            JSONArray array = readAll();
            JSONArray newArray = new JSONArray();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj != null && !id.equals(obj.optString("id"))) {
                    newArray.put(obj);
                }
            }
            writeAll(newArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新一条连接配置（按 id 匹配后替换）
     */
    public void updateConnection(ConnectionItem item) {
        if (item == null || item.id == null) return;
        try {
            JSONArray array = readAll();
            JSONArray newArray = new JSONArray();
            boolean found = false;
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj != null && item.id.equals(obj.optString("id"))) {
                    newArray.put(item.toJson());
                    found = true;
                } else if (obj != null) {
                    newArray.put(obj);
                }
            }
            // 如果没有找到，就当作新增
            if (!found) {
                newArray.put(item.toJson());
            }
            writeAll(newArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取指定类型的所有连接配置
     */
    public List<ConnectionItem> getConnections(String type) {
        List<ConnectionItem> result = new ArrayList<>();
        if (type == null) return result;
        try {
            JSONArray array = readAll();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.optJSONObject(i);
                if (obj != null && type.equals(obj.optString("type"))) {
                    result.add(ConnectionItem.fromJson(obj));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    // ======================== 内部存储方法 ========================

    private JSONArray readAll() {
        try {
            String json = prefs.getString(KEY_DATA, "[]");
            return new JSONArray(json);
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONArray();
        }
    }

    private void writeAll(JSONArray array) {
        try {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_DATA, array.toString());
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
