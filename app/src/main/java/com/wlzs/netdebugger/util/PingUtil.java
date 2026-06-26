package com.wlzs.netdebugger.util;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PingUtil {

    public interface PingListener {
        void onLine(String line);
        void onStats(String stats);
        void onError(String error);
        void onFinished();
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean running = false;

    public void startPing(String host, int count, boolean continuous, PingListener listener) {
        running = true;
        executor.execute(() -> {
            try {
                String command;
                if (continuous || count <= 0) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        command = "ping -c 1000 -i 1 " + host;
                    } else {
                        command = "ping " + host;
                    }
                } else {
                    command = "ping -c " + count + " " + host;
                }

                Process process = Runtime.getRuntime().exec(command);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                String line;
                int sent = 0, received = 0;
                List<Double> delays = new ArrayList<>();

                while (running) {
                    line = reader.readLine();
                    if (line == null) break;

                    handler.post(() -> listener.onLine(line));

                    // Parse ping output
                    if (line.contains("bytes from")) {
                        sent++;
                        // Extract time
                        String[] parts = line.split("time=");
                        if (parts.length > 1) {
                            String timeStr = parts[1].split(" ")[0];
                            try {
                                delays.add(Double.parseDouble(timeStr));
                                received++;
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }

                if (!running) {
                    process.destroy();
                }

                // Wait for process to finish
                String errLine;
                while ((errLine = errorReader.readLine()) != null) {
                    handler.post(() -> listener.onError(errLine));
                }

                // Generate stats
                final int fSent = sent;
                final int fReceived = received;
                final List<Double> fDelays = delays;

                handler.post(() -> {
                    String loss = fSent > 0 ? String.format("%.1f%%", ((fSent - fReceived) * 100.0 / fSent)) : "N/A";
                    String avgDelay = "N/A";
                    if (!fDelays.isEmpty()) {
                        double sum = 0;
                        for (double d : fDelays) sum += d;
                        avgDelay = String.format("%.1f ms", sum / fDelays.size());
                    }
                    String stats = String.format("已发送: %d | 已接收: %d | 丢包率: %s | 平均延迟: %s",
                            fSent, fReceived, loss, avgDelay);
                    listener.onStats(stats);
                    listener.onFinished();
                });

            } catch (Exception e) {
                handler.post(() -> listener.onError(e.getMessage()));
                handler.post(listener::onFinished);
            }
        });
    }

    public void stopPing() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
}
