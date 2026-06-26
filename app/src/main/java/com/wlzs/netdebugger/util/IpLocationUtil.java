package com.wlzs.netdebugger.util;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IpLocationUtil {

    private static List<IpRange> ipRanges = null;

    private static class IpRange implements Comparable<IpRange> {
        long start;
        long end;
        String province;
        String city;
        String isp;

        IpRange(long start, long end, String province, String city, String isp) {
            this.start = start;
            this.end = end;
            this.province = province;
            this.city = city;
            this.isp = isp;
        }

        @Override
        public int compareTo(IpRange other) {
            return Long.compare(this.start, other.start);
        }
    }

    public static synchronized void loadDatabase(Context context) {
        if (ipRanges != null) return;
        ipRanges = new ArrayList<>();
        try {
            InputStream is = context.getAssets().open("china_region.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONArray array = new JSONArray(sb.toString());
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                long start = obj.optLong("start", 0);
                long end = obj.optLong("end", 0);
                String province = obj.optString("province", "");
                String city = obj.optString("city", "");
                String isp = obj.optString("isp", "");
                ipRanges.add(new IpRange(start, end, province, city, isp));
            }
            Collections.sort(ipRanges);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String lookup(Context context, String ip) {
        loadDatabase(context);
        if (ipRanges == null || ipRanges.isEmpty()) return "数据库加载失败";

        try {
            long ipLong = ipToLong(ip);
            // Binary search
            int low = 0, high = ipRanges.size() - 1;
            while (low <= high) {
                int mid = (low + high) / 2;
                IpRange range = ipRanges.get(mid);
                if (ipLong >= range.start && ipLong <= range.end) {
                    StringBuilder result = new StringBuilder();
                    if (!range.province.isEmpty()) result.append(range.province);
                    if (!range.city.isEmpty() && !range.city.equals(range.province)) {
                        result.append(" ").append(range.city);
                    }
                    if (!range.isp.isEmpty()) {
                        result.append(" ").append(range.isp);
                    }
                    return result.length() > 0 ? result.toString() : "本地网络";
                }
                if (ipLong < range.start) {
                    high = mid - 1;
                } else {
                    low = mid + 1;
                }
            }
        } catch (Exception e) {
            return "解析失败: " + e.getMessage();
        }

        return "未知归属地";
    }

    private static long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return 0;
        return ((Long.parseLong(parts[0]) & 0xFFL) << 24) |
                ((Long.parseLong(parts[1]) & 0xFFL) << 16) |
                ((Long.parseLong(parts[2]) & 0xFFL) << 8) |
                (Long.parseLong(parts[3]) & 0xFFL);
    }
}
