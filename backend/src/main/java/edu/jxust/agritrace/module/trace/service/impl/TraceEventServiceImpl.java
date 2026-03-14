package edu.jxust.agritrace.module.trace.service.impl;

import edu.jxust.agritrace.common.exception.BizException;
import edu.jxust.agritrace.common.security.SecurityUtils;
import edu.jxust.agritrace.module.batch.entity.TraceBatch;
import edu.jxust.agritrace.module.batch.mapper.TraceBatchMapper;
import edu.jxust.agritrace.module.trace.dto.TraceEventCreateDTO;
import edu.jxust.agritrace.module.trace.dto.TraceEventListQueryDTO;
import edu.jxust.agritrace.module.trace.entity.TraceEvent;
import edu.jxust.agritrace.module.trace.mapper.TraceEventMapper;
import edu.jxust.agritrace.module.trace.service.TraceEventService;
import edu.jxust.agritrace.module.trace.vo.PublicTraceTimelineVO;
import edu.jxust.agritrace.module.trace.vo.TraceEventItemVO;
import edu.jxust.agritrace.module.trace.vo.TraceNodeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 溯源事件服务实现
 */
@Service
public class TraceEventServiceImpl implements TraceEventService {

    private static final Set<String> ALLOWED_BIZ_ROLES = Set.of(
            "PRODUCER",
            "FARMER",
            "TRANSPORTER",
            "PROCESSOR",
            "WAREHOUSE",
            "SELLER",
            "INSPECTOR",
            "REGULATOR",
            "SYSTEM"
    );

    private final TraceEventMapper traceEventMapper;
    private final TraceBatchMapper traceBatchMapper;

    public TraceEventServiceImpl(TraceEventMapper traceEventMapper,
                                 TraceBatchMapper traceBatchMapper) {
        this.traceEventMapper = traceEventMapper;
        this.traceBatchMapper = traceBatchMapper;
    }

    /**
     * 新增溯源事件
     * 规则：
     * 1. 只能给自己企业的批次新增事件
     * 2. 企业侧新增的 sourceType 固定为 ADMIN
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TraceEventCreateDTO dto) {
        Long companyId = SecurityUtils.getCompanyId();
        Long userId = SecurityUtils.getUserId();
        String realName = SecurityUtils.getLoginUser().getRealName();

        TraceBatch batch = traceBatchMapper.selectById(dto.getBatchId());
        if (batch == null) {
            throw new BizException("批次不存在");
        }

        if (companyId == null || !companyId.equals(batch.getCompanyId())) {
            throw new BizException("无权操作该批次");
        }

        TraceEvent event = new TraceEvent();
        event.setBatchId(dto.getBatchId());
        event.setCompanyId(companyId);
        event.setStage(dto.getStage());
        event.setBizRole(normalizeBizRole(dto.getBizRole(), dto.getStage()));
        event.setTitle(dto.getTitle());
        event.setEventTime(dto.getEventTime());
        event.setOperatorId(userId);
        event.setOperatorName(realName);
        event.setLocation(dto.getLocation());
        event.setSourceType("ADMIN");
        event.setIsPublic(dto.getIsPublic());
        event.setContentJson(dto.getContentJson());
        event.setAttachmentsJson(dto.getAttachmentsJson());

        traceEventMapper.insert(event);
        return event.getId();
    }

    /**
     * 查询后台事件列表
     * 规则：
     * 1. 如果传了 batchId，则校验该批次必须属于当前企业
     */
    @Override
    public List<TraceEventItemVO> list(TraceEventListQueryDTO dto) {
        if (dto.getBatchId() != null) {
            TraceBatch batch = traceBatchMapper.selectById(dto.getBatchId());
            if (batch == null) {
                throw new BizException("批次不存在");
            }

            Long companyId = SecurityUtils.getCompanyId();
            if (companyId == null || !companyId.equals(batch.getCompanyId())) {
                throw new BizException("无权查看该批次事件");
            }
        }

        return traceEventMapper.selectAdminList(dto.getBatchId(), dto.getStage(), dto.getSourceType());
    }

    /**
     * 删除事件
     * 规则：
     * 1. 只能删除自己企业批次下的事件
     * 2. SYSTEM / REGULATOR 事件不允许企业侧删除
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        TraceEvent event = traceEventMapper.selectById(id);
        if (event == null) {
            throw new BizException("事件不存在");
        }

        TraceBatch batch = traceBatchMapper.selectById(event.getBatchId());
        if (batch == null) {
            throw new BizException("批次不存在");
        }

        Long companyId = SecurityUtils.getCompanyId();
        if (companyId == null || !companyId.equals(batch.getCompanyId())) {
            throw new BizException("无权删除该事件");
        }

        if ("SYSTEM".equals(event.getSourceType()) || "REGULATOR".equals(event.getSourceType())) {
            throw new BizException("系统事件或监管事件不允许删除");
        }

        traceEventMapper.deleteById(id);
    }

    /**
     * 前台时间轴
     * 规则：
     * 1. 仅返回 is_public = 1 的事件
     */
    @Override
    public List<PublicTraceTimelineVO> publicTimeline(Long batchId) {
        TraceBatch batch = traceBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BizException("批次不存在");
        }

        return traceEventMapper.selectPublicTimeline(batchId);
    }

    @Override
    public List<TraceNodeVO> nodeList(Long batchId) {
        TraceBatch batch = traceBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BizException("批次不存在");
        }
        return traceEventMapper.selectNodeList(batchId, false);
    }

    @Override
    public List<TraceNodeVO> publicNodeList(Long batchId) {
        TraceBatch batch = traceBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BizException("批次不存在");
        }
        return traceEventMapper.selectNodeList(batchId, true);
    }

    private String normalizeBizRole(String bizRole, String stage) {
        if (bizRole != null && !bizRole.isBlank()) {
            String normalized = bizRole.trim().toUpperCase();
            if (!ALLOWED_BIZ_ROLES.contains(normalized)) {
                throw new BizException("非法业务角色: " + bizRole);
            }
            return normalized;
        }
        if (stage == null) {
            return null;
        }
        return switch (stage.trim().toUpperCase()) {
            case "PRODUCE" -> "PRODUCER";
            case "TRANSPORT" -> "TRANSPORTER";
            case "PROCESS" -> "PROCESSOR";
            case "WAREHOUSE" -> "WAREHOUSE";
            case "SALE" -> "SELLER";
            case "INSPECT" -> "INSPECTOR";
            case "SYSTEM" -> "SYSTEM";
            default -> null;
        };
    }
}
