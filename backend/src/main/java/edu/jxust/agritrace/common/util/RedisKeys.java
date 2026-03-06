package edu.jxust.agritrace.common.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Redis Key 规范：
 * - PV 总计：trace:qr:pv:{qrId}
 * - PV 日增量：trace:qr:pv:day:{yyyyMMdd} (Hash: field=qrId, value=delta)
 * - UV（可选）：trace:qr:uv:{qrId}:{yyyyMMdd} (Set: ip)
 */
public final class RedisKeys {

    private RedisKeys() {}

    public static String qrPv(long qrId) {
        return "trace:qr:pv:" + qrId;
    }

    public static String qrPvDay(LocalDate day) {
        return "trace:qr:pv:day:" + day.format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    public static String qrUv(long qrId, LocalDate day) {
        return "trace:qr:uv:" + qrId + ":" + day.format(DateTimeFormatter.BASIC_ISO_DATE);
    }
}