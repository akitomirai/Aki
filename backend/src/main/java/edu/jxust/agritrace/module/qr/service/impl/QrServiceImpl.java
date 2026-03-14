package edu.jxust.agritrace.module.qr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jxust.agritrace.common.exception.BizException;
import edu.jxust.agritrace.common.security.SecurityUtils;
import edu.jxust.agritrace.module.batch.entity.TraceBatch;
import edu.jxust.agritrace.module.batch.mapper.TraceBatchMapper;
import edu.jxust.agritrace.module.qr.dto.QrGenerateDTO;
import edu.jxust.agritrace.module.qr.entity.QrCode;
import edu.jxust.agritrace.module.qr.entity.QrQueryLog;
import edu.jxust.agritrace.module.qr.entity.QrQueryStatDay;
import edu.jxust.agritrace.module.qr.mapper.QrCodeMapper;
import edu.jxust.agritrace.module.qr.mapper.QrQueryLogMapper;
import edu.jxust.agritrace.module.qr.mapper.QrQueryStatDayMapper;
import edu.jxust.agritrace.module.qr.service.QrService;
import edu.jxust.agritrace.module.qr.vo.PublicBatchSimpleVO;
import edu.jxust.agritrace.module.qr.vo.PublicQrScanVO;
import edu.jxust.agritrace.module.qr.vo.QrCodeVO;
import edu.jxust.agritrace.module.qr.vo.QrDashboardStatsVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class QrServiceImpl implements QrService {

    private static final String KEY_PREFIX = "agri:trace";

    private final QrCodeMapper qrCodeMapper;
    private final QrQueryLogMapper qrQueryLogMapper;
    private final QrQueryStatDayMapper qrQueryStatDayMapper;
    private final TraceBatchMapper traceBatchMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public QrServiceImpl(QrCodeMapper qrCodeMapper,
                         QrQueryLogMapper qrQueryLogMapper,
                         QrQueryStatDayMapper qrQueryStatDayMapper,
                         TraceBatchMapper traceBatchMapper,
                         StringRedisTemplate stringRedisTemplate) {
        this.qrCodeMapper = qrCodeMapper;
        this.qrQueryLogMapper = qrQueryLogMapper;
        this.qrQueryStatDayMapper = qrQueryStatDayMapper;
        this.traceBatchMapper = traceBatchMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generate(QrGenerateDTO dto) {
        Long companyId = SecurityUtils.getCompanyId();
        Long userId = SecurityUtils.getUserId();

        TraceBatch batch = traceBatchMapper.selectById(dto.getBatchId());
        if (batch == null) {
            throw new BizException("批次不存在");
        }
        if (companyId == null || !companyId.equals(batch.getCompanyId())) {
            throw new BizException("无权为该批次生成二维码");
        }

        QrCode qrCode = new QrCode();
        qrCode.setBatchId(dto.getBatchId());
        qrCode.setQrToken(generateToken(dto.getBatchId()));
        qrCode.setStatus("ACTIVE");
        qrCode.setExpiredAt(dto.getExpiredAt());
        qrCode.setRemark(dto.getRemark());
        qrCode.setGeneratedBy(userId);
        qrCode.setPv(0L);

        qrCodeMapper.insert(qrCode);
        return qrCode.getId();
    }

    @Override
    public List<QrCodeVO> listByBatchId(Long batchId) {
        TraceBatch batch = traceBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BizException("批次不存在");
        }

        Long companyId = SecurityUtils.getCompanyId();
        if (companyId == null || !companyId.equals(batch.getCompanyId())) {
            throw new BizException("无权查看该批次二维码");
        }

        List<QrCodeVO> list = qrCodeMapper.selectByBatchId(batchId);
        enrichRealtimeStats(list);
        return list;
    }

    @Override
    public List<QrCodeVO> listByBatchIdForPlatform(Long batchId) {
        TraceBatch batch = traceBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BizException("批次不存在");
        }

        List<QrCodeVO> list = qrCodeMapper.selectByBatchId(batchId);
        enrichRealtimeStats(list);
        return list;
    }

    @Override
    public QrCodeVO detail(Long id) {
        QrCode qrCode = qrCodeMapper.selectById(id);
        if (qrCode == null) {
            throw new BizException("二维码不存在");
        }

        TraceBatch batch = traceBatchMapper.selectById(qrCode.getBatchId());
        if (batch == null) {
            throw new BizException("批次不存在");
        }

        Long companyId = SecurityUtils.getCompanyId();
        if (companyId == null || !companyId.equals(batch.getCompanyId())) {
            throw new BizException("无权查看该二维码");
        }

        QrCodeVO vo = new QrCodeVO();
        BeanUtils.copyProperties(qrCode, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        QrCode qrCode = qrCodeMapper.selectById(id);
        if (qrCode == null) {
            throw new BizException("二维码不存在");
        }

        TraceBatch batch = traceBatchMapper.selectById(qrCode.getBatchId());
        if (batch == null) {
            throw new BizException("批次不存在");
        }

        Long companyId = SecurityUtils.getCompanyId();
        if (companyId == null || !companyId.equals(batch.getCompanyId())) {
            throw new BizException("无权停用该二维码");
        }

        qrCode.setStatus("DISABLED");
        qrCode.setStatusReason("企业管理员手动停用");
        qrCodeMapper.updateById(qrCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PublicQrScanVO scan(String token) {
        QrCode qrCode = qrCodeMapper.selectByToken(token);
        if (qrCode == null) {
            throw new BizException("追溯码不存在或已失效");
        }

        if (!"ACTIVE".equals(qrCode.getStatus())) {
            throw new BizException("追溯码当前不可用");
        }

        if (qrCode.getExpiredAt() != null && qrCode.getExpiredAt().isBefore(LocalDateTime.now())) {
            qrCode.setStatus("EXPIRED");
            qrCode.setStatusReason("二维码已过期");
            qrCodeMapper.updateById(qrCode);
            throw new BizException("追溯码已过期");
        }

        TraceBatch batchEntity = traceBatchMapper.selectById(qrCode.getBatchId());
        if (batchEntity == null) {
            throw new BizException("批次不存在或已删除");
        }

        PublicBatchSimpleVO batch = qrCodeMapper.selectPublicBatchSimple(qrCode.getBatchId());
        if (batch == null) {
            throw new BizException("批次不存在或已删除");
        }

        qrCode.setPv(qrCode.getPv() == null ? 1L : qrCode.getPv() + 1);
        qrCode.setLastQueryAt(LocalDateTime.now());
        qrCodeMapper.updateById(qrCode);

        HttpServletRequest request = currentRequest();
        saveQueryLog(qrCode, request);
        boolean uvAdded = updateRedisAccessStats(qrCode, batchEntity.getCompanyId(), request);
        increaseDailyStat(qrCode.getId(), uvAdded);

        PublicQrScanVO vo = new PublicQrScanVO();
        vo.setQrId(qrCode.getId());
        vo.setQrStatus(qrCode.getStatus());
        vo.setQrStatusReason(qrCode.getStatusReason());
        vo.setBatch(batch);
        return vo;
    }

    @Override
    public QrDashboardStatsVO dashboardStatsForPlatform() {
        return buildDashboardStats(null);
    }

    @Override
    public QrDashboardStatsVO dashboardStatsForEnterprise(Long companyId) {
        return buildDashboardStats(companyId);
    }

    private QrDashboardStatsVO buildDashboardStats(Long companyId) {
        QrDashboardStatsVO vo = new QrDashboardStatsVO();

        Long batchCount = traceBatchMapper.selectCount(
                new LambdaQueryWrapper<TraceBatch>()
                        .eq(companyId != null, TraceBatch::getCompanyId, companyId)
        );
        vo.setBatchCount(batchCount == null ? 0L : batchCount);

        Long qrCount = companyId == null
                ? qrCodeMapper.selectCount(null)
                : qrCodeMapper.countByCompanyId(companyId);
        vo.setQrCount(qrCount == null ? 0L : qrCount);

        if (companyId == null) {
            vo.setPvCount(safeReadCounter(redisPvTotalKey()));
            vo.setUvCount(safeReadSetSize(redisUvTotalKey()));
        } else {
            vo.setPvCount(safeReadCounter(redisPvCompanyKey(companyId)));
            vo.setUvCount(safeReadSetSize(redisUvCompanyKey(companyId)));
        }
        return vo;
    }

    private void enrichRealtimeStats(List<QrCodeVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (QrCodeVO item : list) {
            String token = item.getQrToken();
            if (token == null || token.isBlank()) {
                item.setUv(0L);
                continue;
            }
            Long redisPv = safeReadCounter(redisPvTokenKey(token));
            Long redisUv = safeReadSetSize(redisUvTokenKey(token));
            if (redisPv > 0) {
                item.setPv(redisPv);
            }
            item.setUv(redisUv);
        }
    }

    private String generateToken(Long batchId) {
        String raw = batchId + "." + System.currentTimeMillis() + "." + java.util.UUID.randomUUID();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes());
    }

    private void saveQueryLog(QrCode qrCode, HttpServletRequest request) {
        QrQueryLog log = new QrQueryLog();
        log.setQrId(qrCode.getId());
        log.setBatchId(qrCode.getBatchId());
        log.setQueryTime(LocalDateTime.now());

        if (request != null) {
            log.setIp(resolveClientIp(request));
            log.setUa(request.getHeader("User-Agent"));
            log.setReferer(request.getHeader("Referer"));
        }

        qrQueryLogMapper.insert(log);
    }

    private void increaseDailyStat(Long qrId, boolean uvAdded) {
        LocalDate today = LocalDate.now();
        QrQueryStatDay stat = qrQueryStatDayMapper.selectByQrIdAndDay(qrId, today);

        if (stat == null) {
            stat = new QrQueryStatDay();
            stat.setQrId(qrId);
            stat.setDay(today);
            stat.setPv(1L);
            stat.setUv(uvAdded ? 1L : 0L);
            qrQueryStatDayMapper.insert(stat);
        } else {
            stat.setPv(stat.getPv() == null ? 1L : stat.getPv() + 1);
            if (uvAdded) {
                stat.setUv(stat.getUv() == null ? 1L : stat.getUv() + 1);
            }
            qrQueryStatDayMapper.updateById(stat);
        }
    }

    private boolean updateRedisAccessStats(QrCode qrCode, Long companyId, HttpServletRequest request) {
        String token = qrCode.getQrToken();
        if (token == null || token.isBlank()) {
            return true;
        }

        String visitorId = buildVisitorId(request);
        if (visitorId.isBlank()) {
            visitorId = "anonymous";
        }
        String day = LocalDate.now().toString();

        try {
            stringRedisTemplate.opsForValue().increment(redisPvTokenKey(token));
            stringRedisTemplate.opsForValue().increment(redisPvBatchKey(qrCode.getBatchId()));
            stringRedisTemplate.opsForValue().increment(redisPvTotalKey());
            if (companyId != null) {
                stringRedisTemplate.opsForValue().increment(redisPvCompanyKey(companyId));
            }

            Long tokenUvAdd = stringRedisTemplate.opsForSet().add(redisUvTokenKey(token), visitorId);
            stringRedisTemplate.opsForSet().add(redisUvBatchKey(qrCode.getBatchId()), visitorId);
            stringRedisTemplate.opsForSet().add(redisUvTotalKey(), visitorId);
            if (companyId != null) {
                stringRedisTemplate.opsForSet().add(redisUvCompanyKey(companyId), visitorId);
            }
            Long dayUvAdd = stringRedisTemplate.opsForSet().add(redisUvDayKey(qrCode.getId(), day), visitorId);

            return (dayUvAdd != null && dayUvAdd > 0) || (tokenUvAdd != null && tokenUvAdd > 0);
        } catch (Exception ex) {
            log.warn("Redis stats update failed for token={}, reason={}", token, ex.getMessage());
            return true;
        }
    }

    private Long safeReadCounter(String key) {
        try {
            String val = stringRedisTemplate.opsForValue().get(key);
            if (val == null || val.isBlank()) {
                return 0L;
            }
            return Long.parseLong(val);
        } catch (Exception ex) {
            log.warn("Redis counter read failed for key={}, reason={}", key, ex.getMessage());
            return 0L;
        }
    }

    private Long safeReadSetSize(String key) {
        try {
            Long size = stringRedisTemplate.opsForSet().size(key);
            return size == null ? 0L : size;
        } catch (Exception ex) {
            log.warn("Redis set size read failed for key={}, reason={}", key, ex.getMessage());
            return 0L;
        }
    }

    private String buildVisitorId(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String ip = resolveClientIp(request);
        String ua = request.getHeader("User-Agent");
        String raw = (ip == null ? "" : ip.trim()) + "|" + (ua == null ? "" : ua.trim());
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            if (parts.length > 0 && !parts[0].isBlank()) {
                return parts[0].trim();
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String redisPvTokenKey(String token) {
        return KEY_PREFIX + ":pv:token:" + token;
    }

    private String redisUvTokenKey(String token) {
        return KEY_PREFIX + ":uv:token:" + token;
    }

    private String redisPvBatchKey(Long batchId) {
        return KEY_PREFIX + ":pv:batch:" + batchId;
    }

    private String redisUvBatchKey(Long batchId) {
        return KEY_PREFIX + ":uv:batch:" + batchId;
    }

    private String redisPvCompanyKey(Long companyId) {
        return KEY_PREFIX + ":pv:company:" + companyId;
    }

    private String redisUvCompanyKey(Long companyId) {
        return KEY_PREFIX + ":uv:company:" + companyId;
    }

    private String redisPvTotalKey() {
        return KEY_PREFIX + ":pv:total";
    }

    private String redisUvTotalKey() {
        return KEY_PREFIX + ":uv:total";
    }

    private String redisUvDayKey(Long qrId, String day) {
        return KEY_PREFIX + ":uv:day:" + qrId + ":" + day;
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
