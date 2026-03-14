package edu.jxust.agritrace.module.regulation.service.impl;

import edu.jxust.agritrace.common.exception.BizException;
import edu.jxust.agritrace.common.security.LoginUser;
import edu.jxust.agritrace.common.security.SecurityUtils;
import edu.jxust.agritrace.common.util.NotarySnapshotBuilder;
import edu.jxust.agritrace.module.batch.entity.TraceBatch;
import edu.jxust.agritrace.module.batch.mapper.TraceBatchMapper;
import edu.jxust.agritrace.module.event.service.EventEngineService;
import edu.jxust.agritrace.module.notary.constant.NotaryBizType;
import edu.jxust.agritrace.module.notary.service.HashNotaryService;
import edu.jxust.agritrace.module.regulation.dto.BatchStatusActionDTO;
import edu.jxust.agritrace.module.regulation.dto.RegulationCreateDTO;
import edu.jxust.agritrace.module.regulation.dto.RegulationStatusUpdateDTO;
import edu.jxust.agritrace.module.regulation.entity.RegulationRecord;
import edu.jxust.agritrace.module.regulation.mapper.RegulationRecordMapper;
import edu.jxust.agritrace.module.regulation.service.RegulationService;
import edu.jxust.agritrace.module.regulation.vo.RegulationRecordVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 监管服务实现
 */
@Service
public class RegulationServiceImpl implements RegulationService {

    private final RegulationRecordMapper regulationRecordMapper;
    private final TraceBatchMapper traceBatchMapper;
    private final EventEngineService eventEngineService;
    private final HashNotaryService hashNotaryService;

    public RegulationServiceImpl(RegulationRecordMapper regulationRecordMapper,
                                 TraceBatchMapper traceBatchMapper,
                                 EventEngineService eventEngineService,
                                 HashNotaryService hashNotaryService) {
        this.regulationRecordMapper = regulationRecordMapper;
        this.traceBatchMapper = traceBatchMapper;
        this.eventEngineService = eventEngineService;
        this.hashNotaryService = hashNotaryService;
    }

    /**
     * 新增监管记录
     * 规则：
     * 1. 仅监管员可操作
     * 2. 批次必须存在
     * 3. inspectTime / inspectResult 不能为空
     * 4. 成功后自动生成“监管抽检”事件
     * 5. 新增成功后写入监管记录摘要存证
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRecord(RegulationCreateDTO dto) {
        if (dto.getBatchId() == null) {
            throw new BizException("批次ID不能为空");
        }
        if (dto.getInspectTime() == null) {
            throw new BizException("检查时间不能为空");
        }
        if (isBlank(dto.getInspectResult())) {
            throw new BizException("检查结果不能为空");
        }

        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new BizException("当前登录信息无效");
        }
        if (!"REGULATOR".equals(loginUser.getRoleCode())) {
            throw new BizException("仅监管员可新增监管记录");
        }

        TraceBatch batch = traceBatchMapper.selectById(dto.getBatchId());
        if (batch == null) {
            throw new BizException("批次不存在");
        }

        RegulationRecord record = new RegulationRecord();
        record.setBatchId(dto.getBatchId());
        record.setInspectorId(loginUser.getUserId());
        record.setInspectorName(loginUser.getRealName());
        record.setInspectTime(dto.getInspectTime());
        record.setInspectResult(dto.getInspectResult());
        record.setActionTaken(dto.getActionTaken());
        record.setRemark(dto.getRemark());
        record.setAttachmentUrl(dto.getAttachmentUrl());

        regulationRecordMapper.insert(record);

        eventEngineService.createRegulatorEvent(
                dto.getBatchId(),
                "监管抽检",
                String.format(
                        "{\"remark\":\"%s\",\"actionTaken\":\"%s\",\"inspectResult\":\"%s\"}",
                        safe(dto.getRemark()), safe(dto.getActionTaken()), safe(dto.getInspectResult())
                )
        );

        hashNotaryService.notarize(
                NotaryBizType.REGULATION_RECORD,
                record.getId(),
                NotarySnapshotBuilder.buildRegulationSnapshot(record),
                loginUser.getUserId(),
                "监管检查记录存证"
        );

        return record.getId();
    }

    /**
     * 查询监管记录列表
     * 规则：
     * 1. 仅监管员可查看
     * 2. 批次必须存在
     */
    @Override
    public List<RegulationRecordVO> listByBatchId(Long batchId) {
        if (batchId == null) {
            throw new BizException("批次ID不能为空");
        }

        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new BizException("当前登录信息无效");
        }
        if (!"REGULATOR".equals(loginUser.getRoleCode())) {
            throw new BizException("仅监管员可查看监管记录");
        }

        TraceBatch batch = traceBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BizException("批次不存在");
        }

        return regulationRecordMapper.selectListByBatchId(batchId);
    }

    @Override
    public List<RegulationRecordVO> listPublicByBatchId(Long batchId) {
        if (batchId == null) {
            throw new BizException("批次ID不能为空");
        }

        TraceBatch batch = traceBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BizException("批次不存在");
        }

        return regulationRecordMapper.selectPublicListByBatchId(batchId);
    }

    /**
     * 更新监管状态
     * 规则：
     * 1. 仅监管员可操作
     * 2. 监管状态不能为空
     * 3. 不允许重复更新为同一状态
     * 4. 若更新为 RECALLED，则联动批次主状态为 RECALLED
     * 5. 自动生成“监管状态更新”事件
     * 6. 若监管状态改为 RECALLED，再补一条“监管召回”事件
     * 7. 更新后写入批次摘要存证
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRegulationStatus(RegulationStatusUpdateDTO dto) {
        if (dto.getBatchId() == null) {
            throw new BizException("批次ID不能为空");
        }
        if (isBlank(dto.getRegulationStatus())) {
            throw new BizException("监管状态不能为空");
        }

        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new BizException("当前登录信息无效");
        }
        if (!"REGULATOR".equals(loginUser.getRoleCode())) {
            throw new BizException("仅监管员可更新监管状态");
        }

        TraceBatch batch = traceBatchMapper.selectById(dto.getBatchId());
        if (batch == null) {
            throw new BizException("批次不存在");
        }

        String oldRegulationStatus = batch.getRegulationStatus();
        if (dto.getRegulationStatus().equals(oldRegulationStatus)) {
            throw new BizException("监管状态未发生变化，请勿重复提交");
        }

        // 若批次主状态已召回，则监管状态只能继续保持/更新为 RECALLED
        if ("RECALLED".equals(batch.getStatus()) && !"RECALLED".equals(dto.getRegulationStatus())) {
            throw new BizException("批次已召回，监管状态不能改为非 RECALLED");
        }

        batch.setRegulationStatus(dto.getRegulationStatus());
        batch.setUpdatedBy(loginUser.getUserId());

        // 若监管状态直接变为 RECALLED，则联动主状态
        if ("RECALLED".equals(dto.getRegulationStatus())) {
            if (!"RECALLED".equals(batch.getStatus())) {
                batch.setStatus("RECALLED");
                batch.setRecalledAt(LocalDateTime.now());
            }
            batch.setStatusReason(dto.getReason());
        }

        traceBatchMapper.updateById(batch);

        eventEngineService.createRegulatorEvent(
                dto.getBatchId(),
                "监管状态更新",
                String.format(
                        "{\"message\":\"监管状态由 %s 变更为 %s，原因：%s\"}",
                        safe(oldRegulationStatus), safe(dto.getRegulationStatus()), safe(dto.getReason())
                )
        );

        if ("RECALLED".equals(dto.getRegulationStatus())) {
            eventEngineService.createRegulatorEvent(
                    dto.getBatchId(),
                    "监管召回",
                    "{\"message\":\"监管状态已更新为 RECALLED，批次主状态已联动变更为 RECALLED\"}"
            );
        }

        hashNotaryService.notarize(
                NotaryBizType.TRACE_BATCH,
                batch.getId(),
                NotarySnapshotBuilder.buildBatchSnapshot(batch),
                loginUser.getUserId(),
                "监管状态更新触发批次存证"
        );
    }

    /**
     * 冻结批次
     * 规则：
     * 1. 仅监管员可操作
     * 2. 已召回批次不能再冻结
     * 3. 已冻结批次不能重复冻结
     * 4. 冻结后主状态变为 FROZEN
     * 5. 监管状态变为 RISK
     * 6. 自动生成“批次冻结”系统事件
     * 7. 冻结后写入批次摘要存证
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void freezeBatch(BatchStatusActionDTO dto) {
        if (dto.getBatchId() == null) {
            throw new BizException("批次ID不能为空");
        }

        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new BizException("当前登录信息无效");
        }
        if (!"REGULATOR".equals(loginUser.getRoleCode())) {
            throw new BizException("仅监管员可冻结批次");
        }

        TraceBatch batch = traceBatchMapper.selectById(dto.getBatchId());
        if (batch == null) {
            throw new BizException("批次不存在");
        }

        if ("RECALLED".equals(batch.getStatus())) {
            throw new BizException("已召回批次不能再冻结");
        }
        if ("FROZEN".equals(batch.getStatus())) {
            throw new BizException("批次已冻结，请勿重复操作");
        }

        String oldStatus = batch.getStatus();

        batch.setStatus("FROZEN");
        batch.setRegulationStatus("RISK");
        batch.setFrozenAt(LocalDateTime.now());
        batch.setStatusReason(dto.getReason());
        batch.setUpdatedBy(loginUser.getUserId());
        traceBatchMapper.updateById(batch);

        eventEngineService.createSystemEvent(
                dto.getBatchId(),
                "批次冻结",
                String.format(
                        "{\"message\":\"批次状态由 %s 变更为 FROZEN，原因：%s\"}",
                        safe(oldStatus), safe(dto.getReason())
                )
        );

        hashNotaryService.notarize(
                NotaryBizType.TRACE_BATCH,
                batch.getId(),
                NotarySnapshotBuilder.buildBatchSnapshot(batch),
                loginUser.getUserId(),
                "监管冻结批次存证"
        );
    }

    /**
     * 召回批次
     * 规则：
     * 1. 仅监管员可操作
     * 2. 已召回批次不能重复召回
     * 3. 主状态变为 RECALLED
     * 4. 监管状态联动为 RECALLED
     * 5. 自动生成“批次召回”系统事件
     * 6. 召回后写入批次摘要存证
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recallBatch(BatchStatusActionDTO dto) {
        if (dto.getBatchId() == null) {
            throw new BizException("批次ID不能为空");
        }

        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) {
            throw new BizException("当前登录信息无效");
        }
        if (!"REGULATOR".equals(loginUser.getRoleCode())) {
            throw new BizException("仅监管员可召回批次");
        }

        TraceBatch batch = traceBatchMapper.selectById(dto.getBatchId());
        if (batch == null) {
            throw new BizException("批次不存在");
        }

        if ("RECALLED".equals(batch.getStatus())) {
            throw new BizException("批次已召回，请勿重复操作");
        }

        String oldStatus = batch.getStatus();

        batch.setStatus("RECALLED");
        batch.setRegulationStatus("RECALLED");
        batch.setRecalledAt(LocalDateTime.now());
        batch.setStatusReason(dto.getReason());
        batch.setUpdatedBy(loginUser.getUserId());
        traceBatchMapper.updateById(batch);

        eventEngineService.createSystemEvent(
                dto.getBatchId(),
                "批次召回",
                String.format(
                        "{\"message\":\"批次状态由 %s 变更为 RECALLED，原因：%s\"}",
                        safe(oldStatus), safe(dto.getReason())
                )
        );

        hashNotaryService.notarize(
                NotaryBizType.TRACE_BATCH,
                batch.getId(),
                NotarySnapshotBuilder.buildBatchSnapshot(batch),
                loginUser.getUserId(),
                "监管召回批次存证"
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}