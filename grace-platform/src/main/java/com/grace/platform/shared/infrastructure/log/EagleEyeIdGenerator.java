package com.grace.platform.shared.infrastructure.log;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 生成 30 位 EagleEye 风格 traceId。
 * 格式：IP(8 hex) + 时间戳(13 位十进制 ms) + 自增序号(4 位) + pid 末 2 位(2 位) + 随机扩展(3 位)
 */
public final class EagleEyeIdGenerator {

    private static final String IP_PREFIX;
    private static final String PID_SUFFIX;
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    static {
        IP_PREFIX = resolveIpHex();
        PID_SUFFIX = resolvePidHex();
    }

    private EagleEyeIdGenerator() {
    }

    public static String next() {
        long ts = System.currentTimeMillis();                      // 13
        int seq = SEQ.getAndUpdate(x -> (x + 1) % 10000);          // 4
        int ext = (int) (Math.random() * 0xFFF);                   // 3
        return IP_PREFIX
                + ts
                + String.format("%04d", seq)
                + PID_SUFFIX
                + String.format("%03x", ext);
    }

    private static String resolveIpHex() {
        try {
            byte[] ip = InetAddress.getLocalHost().getAddress();
            StringBuilder sb = new StringBuilder(8);
            for (byte b : ip) {
                sb.append(String.format("%02x", b & 0xFF));
            }
            return sb.toString();
        } catch (UnknownHostException e) {
            return "7f000001"; // 127.0.0.1 降级
        }
    }

    private static String resolvePidHex() {
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            int pid = Integer.parseInt(name.split("@")[0]);
            return String.format("%02x", pid & 0xFF);
        } catch (Exception e) {
            return "00";
        }
    }
}
