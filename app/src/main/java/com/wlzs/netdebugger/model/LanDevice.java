package com.wlzs.netdebugger.model;

public class LanDevice {
    private String ip;
    private String mac;
    private String hostname;
    private boolean isReachable;

    public LanDevice(String ip, String mac, String hostname, boolean isReachable) {
        this.ip = ip;
        this.mac = mac;
        this.hostname = hostname;
        this.isReachable = isReachable;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public boolean isReachable() {
        return isReachable;
    }

    public void setReachable(boolean reachable) {
        isReachable = reachable;
    }
}
