package edu.jxust.agritrace.module.publicapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.common.util.QrTokenUtil;
import edu.jxust.agritrace.common.util.RedisKeys;
import edu.jxust.agritrace.common.util.RequestUtil;
import edu.jxust.agritrace.module.qr.dto.dto.TraceTimelineResponse;
import edu.jxust.agritrace.module.qr.entity.QrCode;
import edu.jxust.agritrace.module.qr.entity.QrQueryLog;
import edu.jxust.agritrace.module.qr.mapper.QrCodeMapper;
import edu.jxust.agritrace.module.qr.mapper.QrQueryLogMapper;
import edu.jxust.agritrace.module.qr.service.GeoService;
import edu.jxust.agritrace.module.qr.service.QrStatService;
import edu.jxust.agritrace.module.quality.service.QualityReportService;
import edu.jxust.agritrace.module.trace.entity.TraceBatch;
import edu.jxust.agritrace.module.trace.entity.TraceEvent;
import edu.jxust.agritrace.module.trace.mapper.TraceBatchMapper;
import edu.jxust.agritrace.module.trace.mapper.TraceEventMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 公共溯源查询接口（不需要登录）
 */
@RestController
@RequestMapping("/api/public/trace")
public class TracePublicController {

    private final TraceBatchMapper traceBatchMapper;
    private final TraceEventMapper traceEventMapper;
    private final QrCodeMapper qrCodeMapper;
    private final ObjectMapper objectMapper;
    private final QrQueryLogMapper qrQueryLogMapper;
    private final QrStatService qrStatService;
    private final GeoService geoService;
    private final StringRedisTemplate redis;
    private final QualityReportService qualityReportService;

    @Value("${app.qr.hmac-secret}")
    private String qrSecret;

    public TracePublicController(TraceBatchMapper traceBatchMapper,
                                 TraceEventMapper traceEventMapper,
                                 QrCodeMapper qrCodeMapper,
                                 ObjectMapper objectMapper,
                                 QrQueryLogMapper qrQueryLogMapper,
                                 QrStatService qrStatService,
                                 GeoService geoService,
                                 StringRedisTemplate redis,
                                 QualityReportService qualityReportService) {
        this.traceBatchMapper = traceBatchMapper;
        this.traceEventMapper = traceEventMapper;
        this.qrCodeMapper = qrCodeMapper;
        this.objectMapper = objectMapper;
        this.qrQueryLogMapper = qrQueryLogMapper;
        this.qrStatService = qrStatService;
        this.geoService = geoService;
        this.redis = redis;
        this.qualityReportService = qualityReportService;
    }

    /**
     * 按 batchId 查询溯源时间轴（测试/调试接口）
     */
    @GetMapping("/batch/{batchId}")
    public Result<TraceTimelineResponse> byBatchId(@PathVariable Long batchId) {
        TraceBatch batch = traceBatchMapper.selectById(batchId);
        if (batch == null) {
            return Result.fail("批次不存在");
        }

        List<TraceEvent> events = traceEventMapper.selectByBatchIdOrderByTime(batchId);

        TraceTimelineResponse resp = new TraceTimelineResponse();
        resp.setBatch(toBatchDto(batch));
        resp.setEvents(toEventDtos(events));

        return Result.ok(resp);
    }

    /**
     * ✅ 扫码入口：按 qrToken 查询溯源时间轴
     * URL: GET /api/public/trace/{qrToken}
     */
    @GetMapping("/{qrToken}")
    public Result<Map<String, Object>> byQrToken(@PathVariable String qrToken, HttpServletRequest request) {

        // 1) 验签 + 过期校验
        QrTokenUtil.Parsed parsed;
        try {
            parsed = QrTokenUtil.verifyAndParse(qrToken, qrSecret);
        } catch (Exception ex) {
            return Result.fail("二维码无效或已过期");
        }

        long qrId = parsed.qrId();

        // 2) 查二维码状态
        QrCode qr = qrCodeMapper.selectById(qrId);
        if (qr == null) return Result.fail("二维码不存在");
        if (!"ACTIVE".equalsIgnoreCase(qr.getStatus())) return Result.fail("二维码已作废");
        if (qr.getExpiredAt() != null && qr.getExpiredAt().isBefore(LocalDateTime.now())) return Result.fail("二维码已过期");

        // 3) PV +1（走 QrStatService：你已做成可插拔/Redis版）
        long pv = qrStatService.incrAndGetPv(qrId);

        // 4) UV（按 qrId + 当天 + ip 去重）
        String ip = RequestUtil.getClientIp(request);
        LocalDate today = LocalDate.now();
        String uvKey = RedisKeys.qrUv(qrId, today);
        redis.opsForSet().add(uvKey, ip);
        redis.expire(uvKey, 40, TimeUnit.DAYS);
        Long uv = redis.opsForSet().size(uvKey);
        if (uv == null) uv = 0L;

        // 5) 写查询日志
        QrQueryLog log = new QrQueryLog();
        log.setQrId(qrId);
        log.setBatchId(qr.getBatchId());
        log.setQueryTime(LocalDateTime.now());
        log.setIp(ip);
        log.setUa(RequestUtil.getUserAgent(request));
        log.setReferer(RequestUtil.getReferer(request));

        var geo = geoService.resolve(ip);
        log.setGeoCountry(geo.getCountry());
        log.setGeoProvince(geo.getProvince());
        log.setGeoCity(geo.getCity());

        qrQueryLogMapper.insert(log);

        // 6) 查询批次 + 事件，组装时间轴
        Long batchId = qr.getBatchId();
        TraceBatch batch = traceBatchMapper.selectById(batchId);
        if (batch == null) {
            return Result.fail("批次不存在");
        }
        List<TraceEvent> events = traceEventMapper.selectByBatchIdOrderByTime(batchId);

        TraceTimelineResponse resp = new TraceTimelineResponse();
        resp.setBatch(toBatchDto(batch));
        resp.setEvents(toEventDtos(events));

        // 7) 质检校验标识
        boolean qualityVerified = qualityReportService.verifyLatest(batchId);

        Map<String, Object> data = new HashMap<>();
        data.put("timeline", resp);
        data.put("qrId", qrId);
        data.put("pv", pv);
        data.put("uv", uv);
        data.put("qualityVerified", qualityVerified);

        return Result.ok(data);
    }

    /** 批次实体 -> DTO */
    private TraceTimelineResponse.BatchDto toBatchDto(TraceBatch b) {
        TraceTimelineResponse.BatchDto dto = new TraceTimelineResponse.BatchDto();
        dto.setId(b.getId());
        dto.setBatchCode(b.getBatchCode());
        dto.setProductId(b.getProductId());
        dto.setCompanyId(b.getCompanyId());
        dto.setOriginPlace(b.getOriginPlace());
        dto.setStartDate(b.getStartDate() == null ? null : b.getStartDate().toString());
        dto.setStatus(b.getStatus());
        dto.setRemark(b.getRemark());
        return dto;
    }

    /** 事件实体列表 -> DTO列表 */
    private List<TraceTimelineResponse.EventDto> toEventDtos(List<TraceEvent> events) {
        List<TraceTimelineResponse.EventDto> list = new ArrayList<>();
        for (TraceEvent e : events) {
            TraceTimelineResponse.EventDto dto = new TraceTimelineResponse.EventDto();
            dto.setId(e.getId());
            dto.setStage(e.getStage());
            dto.setEventTime(e.getEventTime() == null ? null : e.getEventTime().toString());
            dto.setLocation(e.getLocation());
            dto.setContent(parseJsonSafely(e.getContentJson()));
            dto.setAttachments(parseJsonSafely(e.getAttachmentsJson()));
            list.add(dto);
        }
        return list;
    }

    /** JSON 字符串安全解析 */
    private JsonNode parseJsonSafely(String json) {
        try {
            if (json == null || json.isBlank()) return null;
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            // 这里不能用 Map.of（避免 null NPE），但这个 Map 不会带 null，安全
            return objectMapper.valueToTree(Map.of("parseError", true, "message", ex.getMessage()));
        }
    }
}