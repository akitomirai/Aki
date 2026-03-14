package edu.jxust.agritrace.module.batch.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jxust.agritrace.common.exception.BizException;
import edu.jxust.agritrace.common.security.SecurityUtils;
import edu.jxust.agritrace.common.util.NotarySnapshotBuilder;
import edu.jxust.agritrace.module.batch.dto.BatchCreateDTO;
import edu.jxust.agritrace.module.batch.dto.BatchPageQueryDTO;
import edu.jxust.agritrace.module.batch.dto.BatchUpdateDTO;
import edu.jxust.agritrace.module.batch.entity.TraceBatch;
import edu.jxust.agritrace.module.batch.mapper.TraceBatchMapper;
import edu.jxust.agritrace.module.batch.service.BatchParticipantService;
import edu.jxust.agritrace.module.batch.service.BatchService;
import edu.jxust.agritrace.module.batch.vo.BatchDetailVO;
import edu.jxust.agritrace.module.batch.vo.BatchPageItemVO;
import edu.jxust.agritrace.module.event.service.EventEngineService;
import edu.jxust.agritrace.module.notary.constant.NotaryBizType;
import edu.jxust.agritrace.module.notary.service.HashNotaryService;
import edu.jxust.agritrace.module.product.mapper.ProductMapper;
import edu.jxust.agritrace.module.trace.service.TraceEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 批次服务实现
 */
@Service
public class BatchServiceImpl implements BatchService {

    private final TraceBatchMapper traceBatchMapper;
    private final EventEngineService eventEngineService;
    private final HashNotaryService hashNotaryService;
    private final ProductMapper productMapper;
    private final BatchParticipantService batchParticipantService;
    private final TraceEventService traceEventService;

    public BatchServiceImpl(TraceBatchMapper traceBatchMapper,
                            EventEngineService eventEngineService,
                            HashNotaryService hashNotaryService,
                            ProductMapper productMapper,
                            BatchParticipantService batchParticipantService,
                            TraceEventService traceEventService) {
        this.traceBatchMapper = traceBatchMapper;
        this.eventEngineService = eventEngineService;
        this.hashNotaryService = hashNotaryService;
        this.productMapper = productMapper;
        this.batchParticipantService = batchParticipantService;
        this.traceEventService = traceEventService;
    }

    /**
     * 新增批次
     * 规则：
     * 1. 当前企业管理员只能给自己企业新增批次
     * 2. 批次默认状态为 DRAFT
     * 3. 监管状态默认 NONE
     * 4. 新增成功后自动写入“创建批次”系统事件
     * 5. 新增成功后写入批次摘要存证
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(BatchCreateDTO dto) {
        Long companyId = SecurityUtils.getCompanyId();
        Long userId = SecurityUtils.getUserId();

        if (companyId == null) {
            throw new BizException("当前用户未绑定企业，无法创建批次");
        }

        if (dto.getProductId() == null) {
            throw new BizException("产品不能为空");
        }
        // 校验产品是否存在
        if (productMapper.selectById(dto.getProductId()) == null) {
            throw new BizException("产品不存在");
        }
        if (dto.getStartDate() == null) {
            throw new BizException("开始日期不能为空");
        }

        TraceBatch batch = new TraceBatch();
        batch.setBatchCode(generateBatchCode());
        batch.setProductId(dto.getProductId());
        batch.setCompanyId(companyId);
        batch.setOriginPlace(dto.getOriginPlace());
        batch.setStartDate(dto.getStartDate());
        batch.setStatus("DRAFT");
        batch.setRegulationStatus("NONE");
        batch.setRemark(dto.getRemark());
        batch.setPublicRemark(dto.getPublicRemark());
        batch.setInternalRemark(dto.getInternalRemark());
        batch.setCreatedBy(userId);
        batch.setUpdatedBy(userId);

        traceBatchMapper.insert(batch);

        eventEngineService.createSystemEvent(
                batch.getId(),
                "创建批次",
                "{\"message\":\"批次已创建，当前状态为 DRAFT\"}"
        );

        hashNotaryService.notarize(
                NotaryBizType.TRACE_BATCH,
                batch.getId(),
                NotarySnapshotBuilder.buildBatchSnapshot(batch),
                userId,
                "批次创建存证"
        );

        return batch.getId();
    }

    /**
     * 修改批次
     * 规则：
     * 1. 只能修改自己企业的批次
     * 2. 不允许跨企业修改
     * 3. 企业端不允许直接修改监管状态
     * 4. 企业端仅允许将 DRAFT 发布为 ACTIVE
     * 5. 已冻结/已召回批次不允许企业侧再修改
     * 6. 修改成功后自动写入“修改批次信息”系统事件
     * 7. 若状态由 DRAFT 变为 ACTIVE，则自动写入“批次启用”事件
     * 8. 状态关键变化后写入批次摘要存证
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(BatchUpdateDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("批次ID不能为空");
        }

        Long companyId = SecurityUtils.getCompanyId();
        Long userId = SecurityUtils.getUserId();

        TraceBatch dbBatch = traceBatchMapper.selectById(dto.getId());
        if (dbBatch == null) {
            throw new BizException("批次不存在");
        }

        if (companyId == null || !companyId.equals(dbBatch.getCompanyId())) {
            throw new BizException("无权修改该批次");
        }

        if ("FROZEN".equals(dbBatch.getStatus())) {
            throw new BizException("批次已冻结，企业端不可修改");
        }
        if ("RECALLED".equals(dbBatch.getStatus())) {
            throw new BizException("批次已召回，企业端不可修改");
        }

        // 校验产品是否存在
        if (dto.getProductId() != null && productMapper.selectById(dto.getProductId()) == null) {
            throw new BizException("产品不存在");
        }

        String oldStatus = dbBatch.getStatus();
        boolean publishedNow = false;

        // 只在字段不为 null 时更新
        if (dto.getProductId() != null) {
            dbBatch.setProductId(dto.getProductId());
        }
        if (dto.getOriginPlace() != null) {
            dbBatch.setOriginPlace(dto.getOriginPlace());
        }
        if (dto.getStartDate() != null) {
            dbBatch.setStartDate(dto.getStartDate());
        }
        if (dto.getRemark() != null) {
            dbBatch.setRemark(dto.getRemark());
        }
        if (dto.getPublicRemark() != null) {
            dbBatch.setPublicRemark(dto.getPublicRemark());
        }
        if (dto.getInternalRemark() != null) {
            dbBatch.setInternalRemark(dto.getInternalRemark());
        }

        // 企业端仅允许填写业务侧状态原因；若只是普通更新，不强制必须传
        if (dto.getStatusReason() != null) {
            dbBatch.setStatusReason(dto.getStatusReason());
        }

        dbBatch.setUpdatedBy(userId);

        // 企业端仅允许：DRAFT -> ACTIVE
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            if (!"ACTIVE".equals(dto.getStatus())) {
                throw new BizException("企业端仅允许将批次发布为 ACTIVE");
            }
            if (!"DRAFT".equals(oldStatus)) {
                throw new BizException("仅草稿状态批次允许发布");
            }

            dbBatch.setStatus("ACTIVE");
            publishedNow = true;

            if (dbBatch.getPublishedAt() == null) {
                dbBatch.setPublishedAt(LocalDateTime.now());
            }
        }

        // 企业端禁止直接修改监管状态
        if (dto.getRegulationStatus() != null && !dto.getRegulationStatus().isBlank()) {
            throw new BizException("企业端无权修改监管状态");
        }

        traceBatchMapper.updateById(dbBatch);

        eventEngineService.createSystemEvent(
                dbBatch.getId(),
                "修改批次信息",
                "{\"message\":\"已更新批次基础信息\"}"
        );

        if (publishedNow) {
            eventEngineService.createSystemEvent(
                    dbBatch.getId(),
                    "批次启用",
                    "{\"message\":\"批次状态由 " + safeJson(oldStatus) + " 变更为 ACTIVE，原因：" + safeJson(dbBatch.getStatusReason()) + "\"}"
            );

            hashNotaryService.notarize(
                    NotaryBizType.TRACE_BATCH,
                    dbBatch.getId(),
                    NotarySnapshotBuilder.buildBatchSnapshot(dbBatch),
                    userId,
                    "批次发布存证"
            );
        }
    }

    /**
     * 批次详情
     * 规则：
     * 1. 企业管理员只能看自己企业的批次
     * 2. 当前用户必须绑定企业
     */
    @Override
    public BatchDetailVO detail(Long id) {
        BatchDetailVO detail = traceBatchMapper.selectDetailById(id);
        if (detail == null) {
            throw new BizException("批次不存在或已删除");
        }

        Long companyId = SecurityUtils.getCompanyId();
        if (companyId == null) {
            throw new BizException("当前用户未绑定企业");
        }
        if (!companyId.equals(detail.getCompanyId())) {
            throw new BizException("无权访问该批次详情");
        }

        detail.setParticipants(batchParticipantService.listByBatchId(detail.getId()));
        detail.setNodes(traceEventService.nodeList(detail.getId()));
        return detail;
    }

    /**
     * 批次分页
     * 规则：
     * 1. 企业管理员默认只查自己企业
     * 2. 当前用户必须绑定企业
     */
    @Override
    public IPage<BatchPageItemVO> page(BatchPageQueryDTO dto) {
        Long companyId = SecurityUtils.getCompanyId();
        if (companyId == null) {
            throw new BizException("当前用户未绑定企业");
        }
        Page<BatchPageItemVO> page = new Page<>(dto.getCurrent(), dto.getSize());
        return traceBatchMapper.selectPageList(page, dto, companyId);
    }

    /**
     * 生成批次编码
     * 示例：B20260310123030123
     */
    private String generateBatchCode() {
        return "B" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    /**
     * JSON 内容安全转义
     */
    private String safeJson(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }
}
