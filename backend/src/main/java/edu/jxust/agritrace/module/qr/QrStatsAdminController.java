package edu.jxust.agritrace.module.qr;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.module.qr.dto.QrListItem;
import edu.jxust.agritrace.module.qr.dto.QrPvTrendPoint;
import edu.jxust.agritrace.module.qr.entity.QrCode;
import edu.jxust.agritrace.module.qr.mapper.QrCodeMapper;
import edu.jxust.agritrace.module.qr.mapper.QrQueryLogMapper;
import edu.jxust.agritrace.module.qr.mapper.QrQueryStatDayMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 二维码统计（后台）
 */
@RestController
@RequestMapping("/api/admin/qr")
public class QrStatsAdminController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private final QrCodeMapper qrCodeMapper;
    private final QrQueryLogMapper qrQueryLogMapper;
    private final QrQueryStatDayMapper statDayMapper;

    public QrStatsAdminController(QrCodeMapper qrCodeMapper,
                                  QrQueryLogMapper qrQueryLogMapper,
                                  QrQueryStatDayMapper statDayMapper) {
        this.qrCodeMapper = qrCodeMapper;
        this.qrQueryLogMapper = qrQueryLogMapper;
        this.statDayMapper = statDayMapper;
    }

    /**
     * 二维码列表（按PV倒序，热门在前）
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN','REGULATOR')")
    public Result<List<QrListItem>> list(@RequestParam Long batchId) {
        List<QrCode> list = qrCodeMapper.selectByBatchIdOrderByPv(batchId);

        List<QrListItem> result = new ArrayList<>();
        for (QrCode q : list) {
            QrListItem item = new QrListItem();
            item.setId(q.getId());
            item.setBatchId(q.getBatchId());
            item.setQrToken(q.getQrToken());
            item.setStatus(q.getStatus());
            item.setCreatedAt(q.getCreatedAt());
            item.setExpiredAt(q.getExpiredAt());
            item.setRemark(q.getRemark());

            Long dbPv = q.getPv() == null ? 0L : q.getPv();
            String key = "trace:qr:pv:" + q.getId();
            String v = stringRedisTemplate.opsForValue().get(key);
            Long incPv = (v == null || v.isBlank()) ? 0L : Long.parseLong(v);
            item.setPv(dbPv + incPv);

            Long uv = statDayMapper.selectLatestUv(q.getId());
            item.setUv(uv == null ? 0L : uv);

            result.add(item);
        }

        return Result.ok(result);
    }

    /**
     * PV 趋势（近 N 天）
     */
    @GetMapping("/trend")
    @PreAuthorize("hasAnyRole('ADMIN','REGULATOR')")
    public Result<List<QrPvTrendPoint>> trend(@RequestParam Long qrId,
                                              @RequestParam(defaultValue = "7") int days) {
        if (days < 1) days = 1;
        if (days > 30) days = 30;
        List<QrPvTrendPoint> points = statDayMapper.selectPvTrendFromStat(qrId, days);
        return Result.ok(points);
    }
}