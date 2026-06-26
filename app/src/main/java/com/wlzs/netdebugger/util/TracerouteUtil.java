package com.wlzs.netdebugger.util;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TracerouteUtil {

    public interface TracerouteListener {
        void onHop(int hop, String address, String latency);
        void onError(String error);
        void onFinished();
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean running = false;

    public void startTraceroute(String host, int maxHops, TracerouteListener listener) {
        running = true;
        executor.execute(() -> {
            try {
                String command;
                // Try Linux traceroute with ICMP
                command = "traceroute -m " + maxHops + " -w 2 " + host;

                Process process = Runtime.getRuntime().exec(command);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                String line;
                while (running && (line = reader.readLine()) != null) {
                    parseTracerouteLine(line, listener);
                }

                // Try error stream
                while (running && (line = errorReader.readLine()) != null) {
                    if (line.contains("traceroute") || line.contains("Usage") || line.contains("not found")) {
                        handler.post(() -> listener.onError("traceroute 命令不可用，尝试使用 ping 模拟..."));
                        pingTraceroute(host, maxHops, listener);
                        return;
                    }
                    parseTracerouteLine(line, listener);
                }

                handler.post(listener::onFinished);
            } catch (Exception e) {
                handler.post(() -> listener.onError("Traceroute 失败: " + e.getMessage()));
                handler.post(() -> {
                    // Fallback to ICMP traceroute via ping with TTL
                    pingTraceroute(host, maxHops, listener);
                });
            }
        });
    }

    private void parseTracerouteLine(String line, TracerouteListener listener) {
        // Format: " 1  gateway (192.168.1.1)  0.543 ms  0.678 ms  0.432 ms"
        // or: " 1  192.168.1.1  0.543 ms  0.678 ms  0.432 ms"
        // or: " 1  * * *"
        line = line.trim();
        if (line.isEmpty()) return;

        String[] parts = line.split("\\s+");
        if (parts.length < 2) return;

        try {
            int hop = Integer.parseInt(parts[0]);
            if (parts.length >= 2) {
                String addr = parts[1];
                if (addr.equals("*")) {
                    handler.post(() -> listener.onHop(hop, "*", "*"));
                } else {
                    // Extract IP if in parentheses
                    if (addr.contains("(") && addr.contains(")")) {
                        addr = addr.substring(addr.indexOf("(") + 1, addr.indexOf(")"));
                    }
                    // Find first latency value
                    String latency = "*";
                    for (int i = 2; i < parts.length; i++) {
                        if (parts[i].equals("ms") && i > 2) {
                            latency = parts[i - 1];
                            break;
                        }
                    }
                    final String fAddr = addr;
                    final String fLatency = latency;
                    handler.post(() -> listener.onHop(hop, fAddr, fLatency));
                }
            }
        } catch (NumberFormatException e) {
            // Skip non-hop lines
        }
    }

    private void pingTraceroute(String host, int maxHops, TracerouteListener listener) {
        running = true;
        executor.execute(() -> {
            for (int ttl = 1; ttl <= maxHops && running; ttl++) {
                try {
                    String command = "ping -c 1 -t " + ttl + " -W 2 " + host;
                    Process process = Runtime.getRuntime().exec(command);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    process.waitFor();

                    String line;
                    String addr = "*";
                    String latency = "*";
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("From ")) {
                            String[] p = line.split(" ");
                            for (String s : p) {
                                if (s.contains("(") && s.contains(")")) {
                                    addr = s.substring(s.indexOf("(") + 1, s.indexOf(")"));
                                }
                            }
                        }
                        if (line.contains("bytes from")) {
                            String[] p = line.split(" ");
                            for (int i = 0; i < p.length; i++) {
                                if (p[i].equals("time=") && i + 1 < p.length) {
                                    latency = p[i + 1].replace("ms", "").trim();
                                }
                            }
                            if (addr.equals("*")) {
                                for (String s : p) {
                                    if (s.contains("(") && s.contains(")")) {
                                        addr = s.substring(s.indexOf("(") + 1, s.indexOf(")"));
                                    }
                                }
                            }
                        }
                    }
                    final String fAddr = addr;
                    final String fLatency = latency;
                    final int fTtl = ttl;
                    handler.post(() -> listener.onHop(fTtl, fAddr, fLatency));
                } catch (Exception e) {
                    final int fTtl = ttl;
                    handler.post(() -> listener.onHop(fTtl, "*", "*"));
                }
            }
            handler.post(listener::onFinished);
        });
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
}
