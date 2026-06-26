package com.wlzs.netdebugger.util;

import android.os.Handler;
import android.os.Looper;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class PortScanUtil {

    public interface ScanListener {
        void onPortFound(int port, String service);
        void onProgress(int current, int total);
        void onFinished(List<PortResult> results);
        void onError(String error);
    }

    public static class PortResult {
        public int port;
        public String service;
        public boolean open;

        public PortResult(int port, String service) {
            this.port = port;
            this.service = service;
            this.open = true;
        }
    }

    // Common port names
    private static final String[][] WELL_KNOWN_PORTS = {
            {"21", "FTP"}, {"22", "SSH"}, {"23", "Telnet"}, {"25", "SMTP"},
            {"53", "DNS"}, {"80", "HTTP"}, {"110", "POP3"}, {"143", "IMAP"},
            {"443", "HTTPS"}, {"445", "SMB"}, {"993", "IMAPS"}, {"995", "POP3S"},
            {"1433", "MSSQL"}, {"1521", "Oracle"}, {"3306", "MySQL"},
            {"3389", "RDP"}, {"5432", "PostgreSQL"}, {"5900", "VNC"},
            {"6379", "Redis"}, {"8080", "HTTP-Alt"}, {"8443", "HTTPS-Alt"},
            {"27017", "MongoDB"}
    };

    private final ExecutorService executor;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean scanning = false;

    public PortScanUtil() {
        executor = Executors.newFixedThreadPool(20);
    }

    public static String getServiceName(int port) {
        for (String[] entry : WELL_KNOWN_PORTS) {
            if (Integer.parseInt(entry[0]) == port) return entry[1];
        }
        return "Unknown";
    }

    public void startScan(String host, int startPort, int endPort, int timeout, ScanListener listener) {
        scanning = true;
        final int totalPorts = endPort - startPort + 1;
        final AtomicInteger current = new AtomicInteger(0);
        final List<PortResult> results = new ArrayList<>();

        executor.execute(() -> {
            List<Thread> threads = new ArrayList<>();

            for (int port = startPort; port <= endPort && scanning; port++) {
                final int p = port;
                Thread t = new Thread(() -> {
                    try {
                        Socket socket = new Socket();
                        socket.connect(new InetSocketAddress(host, p), timeout);
                        socket.close();
                        String service = getServiceName(p);
                        PortResult result = new PortResult(p, service);
                        synchronized (results) {
                            results.add(result);
                        }
                        int progress = current.incrementAndGet();
                        handler.post(() -> listener.onPortFound(p, service));
                        handler.post(() -> listener.onProgress(progress, totalPorts));
                    } catch (Exception ignored) {
                        current.incrementAndGet();
                        if (current.get() % 10 == 0) {
                            handler.post(() -> listener.onProgress(current.get(), totalPorts));
                        }
                    }
                });
                t.start();
                threads.add(t);

                // Limit concurrent threads
                if (threads.size() >= 20) {
                    for (Thread thread : threads) {
                        try {
                            thread.join(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    threads.clear();
                }
            }

            // Wait for remaining threads
            for (Thread t : threads) {
                try {
                    t.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            final List<PortResult> finalResults = new ArrayList<>(results);
            handler.post(() -> {
                scanning = false;
                listener.onFinished(finalResults);
            });
        });
    }

    public void stopScan() {
        scanning = false;
    }

    public boolean isScanning() {
        return scanning;
    }
}
