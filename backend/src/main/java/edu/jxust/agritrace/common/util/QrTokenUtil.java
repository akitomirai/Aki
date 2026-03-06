package edu.jxust.agritrace.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * 二维码 Token 工具类：
 * token 结构：base64url("qrId.expireAt.sig")
 * sig = HMACSHA256("qrId.expireAt", secret) 的 base64url
 *
 * 优点：
 * - 二维码中不暴露 batchId
 * - 可防篡改（验签）
 * - 可过期控制
 */
public final class QrTokenUtil {

    private QrTokenUtil() {}

    /** 解析结果 */
    public record Parsed(long qrId, long expireAtEpochSec) {}

    /**
     * 生成 token
     */
    public static String generate(long qrId, long expireAtEpochSec, String secret) {
        String payload = qrId + "." + expireAtEpochSec;
        String sig = hmacSha256Base64Url(payload, secret);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((payload + "." + sig).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 验签并解析 token
     */
    public static Parsed verifyAndParse(String token, String secret) {
        String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        String[] arr = raw.split("\\.");
        if (arr.length != 3) throw new IllegalArgumentException("bad token");

        long qrId = Long.parseLong(arr[0]);
        long exp = Long.parseLong(arr[1]);
        String sig = arr[2];

        String payload = arr[0] + "." + arr[1];
        String expected = hmacSha256Base64Url(payload, secret);

        if (!constantTimeEquals(sig, expected)) throw new IllegalArgumentException("bad signature");
        if (Instant.now().getEpochSecond() > exp) throw new IllegalArgumentException("expired");

        return new Parsed(qrId, exp);
    }

    private static String hmacSha256Base64Url(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}