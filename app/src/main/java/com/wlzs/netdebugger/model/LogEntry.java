package com.wlzs.netdebugger.model;

public class LogEntry {
    public static final int TYPE_SEND = 0;
    public static final int TYPE_RECEIVE = 1;
    public static final int TYPE_ERROR = 2;
    public static final int TYPE_INFO = 3;
    public static final int TYPE_WARN = 4;

    private int type;
    private String message;
    private long timestamp;

    public LogEntry(int type, String message) {
        this.type = type;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public int getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedTime() {
        long ms = timestamp % 1000;
        long s = (timestamp / 1000) % 60;
        long m = (timestamp / 60000) % 60;
        long h = (timestamp / 3600000);
        return String.format("%02d:%02d:%02d.%03d", h, m, s, ms);
    }
}
