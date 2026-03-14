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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * 二维码服务实现
 */
@Service
public class QrServiceImpl implements QrService {

    private final QrCodeMapper qrCodeMapper;
    private final QrQueryLogMapper qrQueryLogMapper;
    private final QrQueryStatDayMapper qrQueryStatDayMapper;
    private final TraceBatchMapper traceBatchMapper;

    public QrServiceImpl(QrCodeMapper qrCodeMapper,
                         QrQueryLogMapper qrQueryLogMapper,
                         QrQueryStatDayMapper qrQueryStatDayMapper,
                         TraceBatchMapper traceBatchMapper) {
        this.qrCodeMapper = qrCodeMapper;
        this.qrQueryLogMapper = qrQueryLogMapper;
        this.qrQueryStatDayMapper = qrQueryStatDayMapper;
        this.traceBatchMapper = traceBatchMapper;
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

        return qrCodeMapper.selectByBatchId(batchId);
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

        PublicBatchSimpleVO batch = qrCodeMapper.selectPublicBatchSimple(qrCode.getBatchId());
        if (batch == null) {
            throw new BizException("批次不存在或已删除");
        }

        // 更新二维码统计
        qrCode.setPv(qrCode.getPv() == null ? 1L : qrCode.getPv() + 1);
        qrCode.setLastQueryAt(LocalDateTime.now());
        qrCodeMapper.updateById(qrCode);

        // 写扫码日志
        saveQueryLog(qrCode);

        // 更新日统计
        increaseDailyStat(qrCode.getId());

        PublicQrScanVO vo = new PublicQrScanVO();
        vo.setQrId(qrCode.getId());
        vo.setQrStatus(qrCode.getStatus());
        vo.setQrStatusReason(qrCode.getStatusReason());
        vo.setBatch(batch);
        return vo;
    }

    /**
     * 生成二维码 token
     */
    private String generateToken(Long batchId) {
        String raw = batchId + "." + System.currentTimeMillis() + "." + java.util.UUID.randomUUID();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes());
    }

    /**
     * 保存扫码日志
     */
    private void saveQueryLog(QrCode qrCode) {
        HttpServletRequest request = currentRequest();

        QrQueryLog log = new QrQueryLog();
        log.setQrId(qrCode.getId());
        log.setBatchId(qrCode.getBatchId());
        log.setQueryTime(LocalDateTime.now());

        if (request != null) {
            log.setIp(request.getRemoteAddr());
            log.setUa(request.getHeader("User-Agent"));
            log.setReferer(request.getHeader("Referer"));
        }

        qrQueryLogMapper.insert(log);
    }

    /**
     * 增加日统计
     */
    private void increaseDailyStat(Long qrId) {
        LocalDate today = LocalDate.now();
        QrQueryStatDay stat = qrQueryStatDayMapper.selectByQrIdAndDay(qrId, today);

        if (stat == null) {
            stat = new QrQueryStatDay();
            stat.setQrId(qrId);
            stat.setDay(today);
            stat.setPv(1L);
            stat.setUv(1L);
            qrQueryStatDayMapper.insert(stat);
        } else {
            stat.setPv(stat.getPv() == null ? 1L : stat.getPv() + 1);
            stat.setUv(stat.getUv() == null ? 1L : stat.getUv() + 1);
            qrQueryStatDayMapper.updateById(stat);
        }
    }

    /**
     * 获取当前请求对象
     */
    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
