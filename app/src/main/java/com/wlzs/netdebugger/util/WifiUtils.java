package com.wlzs.netdebugger.util;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class WifiUtils {

    public static WifiInfo getWifiInfo(Context context) {
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            return wm.getConnectionInfo();
        }
        return null;
    }

    public static String getSSID(Context context) {
        WifiInfo info = getWifiInfo(context);
        if (info != null) {
            String ssid = info.getSSID();
            if (ssid != null) {
                if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid.substring(1, ssid.length() - 1);
                }
                return ssid;
            }
        }
        return "";
    }

    public static String getBSSID(Context context) {
        WifiInfo info = getWifiInfo(context);
        if (info != null) {
            return info.getBSSID();
        }
        return "";
    }

    public static String getIpAddress(Context context) {
        WifiInfo info = getWifiInfo(context);
        if (info != null) {
            int ip = info.getIpAddress();
            return Formatter.formatIpAddress(ip);
        }
        return "";
    }

    public static int getSignalStrength(Context context) {
        WifiInfo info = getWifiInfo(context);
        if (info != null) {
            return info.getRssi();
        }
        return 0;
    }

    public static String getSignalLevel(Context context) {
        int rssi = getSignalStrength(context);
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null && wm.isWifiEnabled()) {
            int level = wm.calculateSignalLevel(rssi);
            int[] levels = new int[]{level * 25, level * 25 + 25, level * 25 + 50, level * 25 + 75};
            return rssi + " dBm (Level " + (level + 1) + "/5)";
        }
        return rssi + " dBm";
    }

    public static int getFrequency(Context context) {
        WifiInfo info = getWifiInfo(context);
        if (info != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            return info.getFrequency();
        }
        return 0;
    }

    public static String getGateway(Context context) {
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            DhcpInfo dhcp = wm.getDhcpInfo();
            if (dhcp != null) {
                return Formatter.formatIpAddress(dhcp.gateway);
            }
        }
        return "";
    }

    public static String getNetmask(Context context) {
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            DhcpInfo dhcp = wm.getDhcpInfo();
            if (dhcp != null) {
                return Formatter.formatIpAddress(dhcp.netmask);
            }
        }
        return "";
    }

    public static List<String> getDnsServers(Context context) {
        List<String> dnsList = new ArrayList<>();
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            DhcpInfo dhcp = wm.getDhcpInfo();
            if (dhcp != null) {
                String dns1 = Formatter.formatIpAddress(dhcp.dns1);
                String dns2 = Formatter.formatIpAddress(dhcp.dns2);
                if (!dns1.equals("0.0.0.0")) dnsList.add(dns1);
                if (!dns2.equals("0.0.0.0")) dnsList.add(dns2);
            }
        }
        return dnsList;
    }

    public static String getSubnetAddress(Context context) {
        String ip = getIpAddress(context);
        if (ip == null || ip.isEmpty()) return "";
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return "";
        return parts[0] + "." + parts[1] + "." + parts[2] + ".";
    }
}
