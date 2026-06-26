package com.wlzs.netdebugger.util;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DnsQueryUtil {

    public interface DnsListener {
        void onResult(List<DnsRecord> records);
        void onError(String error);
    }

    public static class DnsRecord {
        public String name;
        public String type;
        public int typeCode;
        public int ttl;
        public String data;

        public DnsRecord(String name, String type, int typeCode, int ttl, String data) {
            this.name = name;
            this.type = type;
            this.typeCode = typeCode;
            this.ttl = ttl;
            this.data = data;
        }

        @Override
        public String toString() {
            return name + "  " + type + "  TTL:" + ttl + "  " + data;
        }
    }

    private static final String[] RECORD_TYPE_NAMES = {"", "A", "NS", "MD", "MF", "CNAME", "SOA", "MB", "MG", "MR", "NULL",
            "WKS", "PTR", "HINFO", "MINFO", "MX", "TXT"};

    private static final int TYPE_A = 1;
    private static final int TYPE_AAAA = 28;
    private static final int TYPE_CNAME = 5;
    private static final int TYPE_MX = 15;
    private static final int TYPE_NS = 2;
    private static final int TYPE_TXT = 16;
    private static final int TYPE_SOA = 6;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public void query(String domain, String dnsServer, int recordType, DnsListener listener) {
        executor.execute(() -> {
            try {
                DatagramSocket socket = new DatagramSocket();
                socket.setSoTimeout(5000);

                byte[] query = buildDnsQuery(domain, recordType);
                InetAddress serverAddr = InetAddress.getByName(dnsServer);
                DatagramPacket sendPacket = new DatagramPacket(query, query.length, serverAddr, 53);
                socket.send(sendPacket);

                byte[] buffer = new byte[4096];
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(receivePacket);

                List<DnsRecord> records = parseDnsResponse(receivePacket.getData(), receivePacket.getLength());
                handler.post(() -> listener.onResult(records));

                socket.close();
            } catch (SocketTimeoutException e) {
                handler.post(() -> listener.onError("查询超时"));
            } catch (IOException e) {
                handler.post(() -> listener.onError("查询失败: " + e.getMessage()));
            }
        });
    }

    private byte[] buildDnsQuery(String domain, int type) {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        // Header
        buffer.putShort((short) 0x1234); // Transaction ID
        buffer.putShort((short) 0x0100); // Flags: standard query
        buffer.putShort((short) 1);     // Questions
        buffer.putShort((short) 0);     // Answers
        buffer.putShort((short) 0);     // Authority
        buffer.putShort((short) 0);     // Additional

        // Question
        String[] parts = domain.split("\\.");
        for (String part : parts) {
            byte[] bytes = part.getBytes();
            buffer.put((byte) bytes.length);
            buffer.put(bytes);
        }
        buffer.put((byte) 0); // End of name
        buffer.putShort((short) type); // Type
        buffer.putShort((short) 1);    // Class: IN

        byte[] result = new byte(buffer.position());
        buffer.flip();
        buffer.get(result);
        return result;
    }

    private List<DnsRecord> parseDnsResponse(byte[] data, int length) {
        List<DnsRecord> records = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.wrap(data);

        // Skip header (12 bytes)
        buffer.position(12);

        // Skip questions
        int qCount = Short.toUnsignedInt(buffer.getShort(4));
        for (int i = 0; i < qCount; i++) {
            skipName(buffer);
            buffer.getShort(); // type
            buffer.getShort(); // class
        }

        // Parse answers
        int aCount = Short.toUnsignedInt(buffer.getShort(6));
        for (int i = 0; i < aCount; i++) {
            String name = readName(buffer, data);
            int type = Short.toUnsignedInt(buffer.getShort());
            int cls = Short.toUnsignedInt(buffer.getShort());
            long ttl = Integer.toUnsignedLong(buffer.getInt()) & 0xFFFFFFFFL;
            int rdLength = Short.toUnsignedInt(buffer.getShort());

            String typeName = type < RECORD_TYPE_NAMES.length ? RECORD_TYPE_NAMES[type] : "TYPE" + type;
            String recordData;

            switch (type) {
                case TYPE_A:
                    if (rdLength == 4) {
                        recordData = (buffer.get() & 0xFF) + "." +
                                (buffer.get() & 0xFF) + "." +
                                (buffer.get() & 0xFF) + "." +
                                (buffer.get() & 0xFF);
                    } else {
                        recordData = "Invalid A record";
                        buffer.position(buffer.position() + rdLength);
                    }
                    break;
                case TYPE_AAAA:
                    if (rdLength == 16) {
                        StringBuilder sb = new StringBuilder();
                        for (int j = 0; j < 16; j += 2) {
                            if (j > 0) sb.append(":");
                            sb.append(String.format("%x", ((buffer.get() & 0xFF) << 8) | (buffer.get() & 0xFF)));
                        }
                        recordData = sb.toString();
                    } else {
                        recordData = "Invalid AAAA record";
                        buffer.position(buffer.position() + rdLength);
                    }
                    break;
                case TYPE_CNAME:
                case TYPE_NS:
                case TYPE_PTR:
                    recordData = readName(buffer, data);
                    break;
                case TYPE_MX:
                    int preference = Short.toUnsignedInt(buffer.getShort());
                    recordData = preference + " " + readName(buffer, data);
                    break;
                case TYPE_TXT:
                    StringBuilder txtSb = new StringBuilder();
                    int txtEnd = buffer.position() + rdLength;
                    while (buffer.position() < txtEnd) {
                        int txtLen = buffer.get() & 0xFF;
                        byte[] txtBytes = new byte[txtLen];
                        buffer.get(txtBytes);
                        txtSb.append(new String(txtBytes));
                    }
                    recordData = txtSb.toString();
                    break;
                case TYPE_SOA:
                    String mname = readName(buffer, data);
                    String rname = readName(buffer, data);
                    long serial = Integer.toUnsignedLong(buffer.getInt()) & 0xFFFFFFFFL;
                    long refresh = Integer.toUnsignedLong(buffer.getInt()) & 0xFFFFFFFFL;
                    long retry = Integer.toUnsignedLong(buffer.getInt()) & 0xFFFFFFFFL;
                    long expire = Integer.toUnsignedLong(buffer.getInt()) & 0xFFFFFFFFL;
                    long minimum = Integer.toUnsignedLong(buffer.getInt()) & 0xFFFFFFFFL;
                    recordData = "MNAME: " + mname + "\nRNAME: " + rname +
                            "\nSERIAL: " + serial + " REFRESH: " + refresh +
                            " RETRY: " + retry + " EXPIRE: " + expire + " MINIMUM: " + minimum;
                    break;
                default:
                    recordData = "[Binary data, " + rdLength + " bytes]";
                    buffer.position(buffer.position() + rdLength);
                    break;
            }

            records.add(new DnsRecord(name, typeName, type, (int) ttl, recordData));
        }

        return records;
    }

    private void skipName(ByteBuffer buffer) {
        while (true) {
            int len = buffer.get() & 0xFF;
            if (len == 0) break;
            if ((len & 0xC0) == 0xC0) {
                buffer.get();
                break;
            }
            buffer.position(buffer.position() + len);
        }
    }

    private String readName(ByteBuffer buffer, byte[] data) {
        StringBuilder sb = new StringBuilder();
        int savedPos = -1;
        int jumps = 0;
        boolean jumped = false;

        while (true) {
            int len = buffer.get() & 0xFF;
            if (len == 0) break;

            if ((len & 0xC0) == 0xC0) {
                if (!jumped) savedPos = buffer.position();
                int offset = ((len & 0x3F) << 8) | (buffer.get() & 0xFF);
                buffer.position(offset);
                jumped = true;
                if (jumps++ > 10) break;
                continue;
            }

            if (sb.length() > 0) sb.append(".");
            byte[] bytes = new byte[len];
            buffer.get(bytes);
            sb.append(new String(bytes));
        }

        if (jumped) {
            buffer.position(savedPos);
        }

        return sb.toString();
    }

    public static int getTypeCode(String typeName) {
        switch (typeName.toUpperCase()) {
            case "A": return TYPE_A;
            case "AAAA": return TYPE_AAAA;
            case "CNAME": return TYPE_CNAME;
            case "MX": return TYPE_MX;
            case "NS": return TYPE_NS;
            case "TXT": return TYPE_TXT;
            case "SOA": return TYPE_SOA;
            default: return TYPE_A;
        }
    }
}
