package edu.jxust.agritrace.module.qr.service.impl;

import edu.jxust.agritrace.common.util.RedisKeys;
import edu.jxust.agritrace.module.qr.service.QrStatService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * Redis PV 计数实现（高并发）：
 * - PV 总计：INCR trace:qr:pv:{qrId}
 * - PV 日增量：HINCRBY trace:qr:pv:day:{yyyyMMdd} {qrId} 1
 *
 * 开关：app.redis.enabled=true 时启用
 */
@Service
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class RedisQrStatService implements QrStatService {

    private final StringRedisTemplate redis;

    public RedisQrStatService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public long incrAndGetPv(long qrId) {
        // 1) 总PV原子自增
        Long total = redis.opsForValue().increment(RedisKeys.qrPv(qrId));
        if (total == null) total = 0L;

        // 2) 当天增量写入Hash（用于定时落库）
        LocalDate today = LocalDate.now();
        String dayKey = RedisKeys.qrPvDay(today);
        redis.opsForHash().increment(dayKey, String.valueOf(qrId), 1);

        // 3) 给日增量 key 设置过期（避免redis堆积，保留40天够论文）
        redis.expire(dayKey, 40, TimeUnit.DAYS);

        return total;
    }
}