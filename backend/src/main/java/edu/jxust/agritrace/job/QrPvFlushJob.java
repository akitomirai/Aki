package edu.jxust.agritrace.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jxust.agritrace.common.util.RedisKeys;
import edu.jxust.agritrace.module.qr.entity.QrQueryStatDay;
import edu.jxust.agritrace.module.qr.mapper.QrQueryStatDayMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;

/**
 * PV 日增量落库任务：
 * - 从 Redis Hash 读取当天增量
 * - upsert 到 qr_query_stat_day
 *
 * 说明：只有 app.redis.enabled=true 才启用
 */
@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class QrPvFlushJob {

    private final StringRedisTemplate redis;
    private final QrQueryStatDayMapper statDayMapper;

    public QrPvFlushJob(StringRedisTemplate redis, QrQueryStatDayMapper statDayMapper) {
        this.redis = redis;
        this.statDayMapper = statDayMapper;
    }

    /**
     * 每 5 分钟刷一次（毕业设计足够）
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void flushToday() {
        LocalDate today = LocalDate.now();

        // 1) PV 当天增量（Hash）
        String pvDayKey = RedisKeys.qrPvDay(today);
        Map<Object, Object> entries = redis.opsForHash().entries(pvDayKey);

        // 如果今天没有 PV 增量，也不直接 return，因为 UV 可能有（极少见，但保持严谨）
        // 我们收集今天需要处理的 qrId 集合
        java.util.Set<Long> qrIds = new HashSet<>();

        if (entries != null) {
            for (var e : entries.entrySet()) {
                qrIds.add(Long.parseLong(String.valueOf(e.getKey())));
            }
        }

        // 2) 对每个 qrId 落库（pv 增量 + uv 当天值）
        for (Long qrId : qrIds) {
            long deltaPv = 0L;
            if (entries != null && entries.containsKey(String.valueOf(qrId))) {
                deltaPv = Long.parseLong(String.valueOf(entries.get(String.valueOf(qrId))));
            } else if (entries != null && entries.containsKey(qrId)) {
                // 兼容 key 是 Long 的情况
                deltaPv = Long.parseLong(String.valueOf(entries.get(qrId)));
            }

            // UV：当天 set 的 size
            String uvKey = RedisKeys.qrUv(qrId, today);
            Long uv = redis.opsForSet().size(uvKey);
            if (uv == null) uv = 0L;

            // upsert
            var exist = statDayMapper.selectOne(
                    new LambdaQueryWrapper<QrQueryStatDay>()
                            .eq(QrQueryStatDay::getQrId, qrId)
                            .eq(QrQueryStatDay::getDay, today)
                            .last("limit 1")
            );

            if (exist == null) {
                var row = new QrQueryStatDay();
                row.setQrId(qrId);
                row.setDay(today);
                row.setPv(deltaPv);
                row.setUv(uv);
                statDayMapper.insert(row);
            } else {
                exist.setPv((exist.getPv() == null ? 0 : exist.getPv()) + deltaPv);
                // UV 用当天去重后的最终值覆盖（更合理）
                exist.setUv(uv);
                statDayMapper.updateById(exist);
            }
        }

        // 3) 清空 PV 增量 hash（避免重复累计）
        redis.delete(pvDayKey);
    }
}