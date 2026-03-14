package edu.jxust.agritrace.module.feedback.service.impl;

import edu.jxust.agritrace.common.exception.BizException;
import edu.jxust.agritrace.common.security.SecurityUtils;
import edu.jxust.agritrace.module.batch.entity.TraceBatch;
import edu.jxust.agritrace.module.batch.mapper.TraceBatchMapper;
import edu.jxust.agritrace.module.feedback.dto.FeedbackCreateDTO;
import edu.jxust.agritrace.module.feedback.dto.FeedbackHandleDTO;
import edu.jxust.agritrace.module.feedback.entity.ConsumerFeedback;
import edu.jxust.agritrace.module.feedback.mapper.ConsumerFeedbackMapper;
import edu.jxust.agritrace.module.feedback.service.ConsumerFeedbackService;
import edu.jxust.agritrace.module.feedback.vo.FeedbackVO;
import edu.jxust.agritrace.module.qr.entity.QrCode;
import edu.jxust.agritrace.module.qr.mapper.QrCodeMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消费者反馈服务实现
 */
@Service
public class ConsumerFeedbackServiceImpl implements ConsumerFeedbackService {

    private final ConsumerFeedbackMapper consumerFeedbackMapper;
    private final TraceBatchMapper traceBatchMapper;
    private final QrCodeMapper qrCodeMapper;

    public ConsumerFeedbackServiceImpl(ConsumerFeedbackMapper consumerFeedbackMapper,
                                       TraceBatchMapper traceBatchMapper,
                                       QrCodeMapper qrCodeMapper) {
        this.consumerFeedbackMapper = consumerFeedbackMapper;
        this.traceBatchMapper = traceBatchMapper;
        this.qrCodeMapper = qrCodeMapper;
    }

    /**
     * 前台提交反馈
     * 规则：
     * 1. 批次必须存在
     * 2. 如果传了 qrId，则二维码也必须存在
     * 3. 默认来源渠道为 SCAN_PAGE
     * 4. 默认状态为 PENDING
     * 5. 默认不公开
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(FeedbackCreateDTO dto) {
        TraceBatch batch = traceBatchMapper.selectById(dto.getBatchId());
        if (batch == null) {
            throw new BizException("批次不存在");
        }

        if (dto.getQrId() != null) {
            QrCode qrCode = qrCodeMapper.selectById(dto.getQrId());
            if (qrCode == null) {
                throw new BizException("二维码不存在");
            }
        }

        ConsumerFeedback feedback = new ConsumerFeedback();
        BeanUtils.copyProperties(dto, feedback);

        feedback.setSourceChannel("SCAN_PAGE");
        feedback.setStatus("PENDING");
        feedback.setIsPublic(false);

        consumerFeedbackMapper.insert(feedback);
        return feedback.getId();
    }

    /**
     * 后台查询反馈列表
     */
    @Override
    public List<FeedbackVO> list() {
        return consumerFeedbackMapper.selectAllList();
    }

    /**
     * 后台查询反馈详情
     */
    @Override
    public FeedbackVO detail(Long id) {
        FeedbackVO vo = consumerFeedbackMapper.selectDetailById(id);
        if (vo == null) {
            throw new BizException("反馈不存在");
        }
        return vo;
    }

    /**
     * 后台处理反馈
     * 规则：
     * 1. 反馈必须存在
     * 2. 记录处理人和处理时间
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handle(FeedbackHandleDTO dto) {
        ConsumerFeedback feedback = consumerFeedbackMapper.selectById(dto.getId());
        if (feedback == null) {
            throw new BizException("反馈不存在");
        }

        feedback.setStatus(dto.getStatus());
        feedback.setHandleResult(dto.getHandleResult());
        feedback.setHandledBy(SecurityUtils.getUserId());
        feedback.setHandledAt(LocalDateTime.now());

        consumerFeedbackMapper.updateById(feedback);
    }

    /**
     * 后台公开反馈
     * 规则：
     * 1. 反馈必须存在
     * 2. 将 isPublic 设置为 true
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        ConsumerFeedback feedback = consumerFeedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BizException("反馈不存在");
        }

        feedback.setIsPublic(true);
        consumerFeedbackMapper.updateById(feedback);
    }

    /**
     * 企业端查询本企业反馈列表
     */
    @Override
    public List<FeedbackVO> selectEnterpriseList(Long companyId) {
        return consumerFeedbackMapper.selectEnterpriseList(companyId);
    }

    /**
     * 企业端查询本企业反馈详情
     */
    @Override
    public FeedbackVO selectEnterpriseDetail(Long id, Long companyId) {
        FeedbackVO vo = consumerFeedbackMapper.selectEnterpriseDetail(id, companyId);
        if (vo == null) {
            throw new BizException("反馈不存在");
        }
        return vo;
    }

    /**
     * 企业端处理反馈
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleByCompanyId(FeedbackHandleDTO dto, Long companyId) {
        // 先检查反馈是否属于本企业
        FeedbackVO vo = consumerFeedbackMapper.selectEnterpriseDetail(dto.getId(), companyId);
        if (vo == null) {
            throw new BizException("反馈不存在");
        }

        // 处理反馈
        ConsumerFeedback feedback = consumerFeedbackMapper.selectById(dto.getId());
        feedback.setStatus(dto.getStatus());
        feedback.setHandleResult(dto.getHandleResult());
        feedback.setHandledBy(SecurityUtils.getUserId());
        feedback.setHandledAt(LocalDateTime.now());

        consumerFeedbackMapper.updateById(feedback);
    }
}