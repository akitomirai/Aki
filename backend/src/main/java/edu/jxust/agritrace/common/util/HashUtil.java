package edu.jxust.agritrace.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Hash 工具类：SHA-256
 */
public final class HashUtil {

    private HashUtil() {}

    /**
     * 计算字符串的 SHA-256（hex）
     */
    public static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}