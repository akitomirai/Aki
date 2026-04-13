package edu.jxust.agritrace.module.batch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jxust.agritrace.common.exception.ForbiddenException;
import edu.jxust.agritrace.common.exception.UnauthorizedException;
import edu.jxust.agritrace.module.auth.mapper.SysUserMapper;
import edu.jxust.agritrace.module.auth.mapper.po.SysUserPO;
import edu.jxust.agritrace.module.auth.model.AuthUserSession;
import edu.jxust.agritrace.module.batch.dto.BatchCreateRequest;
import edu.jxust.agritrace.module.batch.dto.BatchAssignmentRequest;
import edu.jxust.agritrace.module.batch.dto.BatchListQueryRequest;
import edu.jxust.agritrace.module.batch.dto.BatchRiskActionCreateRequest;
import edu.jxust.agritrace.module.batch.dto.BatchStatusActionRequest;
import edu.jxust.agritrace.module.batch.dto.BatchUpdateRequest;
import edu.jxust.agritrace.module.batch.dto.FieldDraftFileItemDTO;
import edu.jxust.agritrace.module.batch.dto.FieldDraftSaveRequest;
import edu.jxust.agritrace.module.batch.dto.QualityReportCreateRequest;
import edu.jxust.agritrace.module.batch.dto.TraceRecordCreateRequest;
import edu.jxust.agritrace.module.batch.entity.AttachmentBusinessType;
import edu.jxust.agritrace.module.batch.entity.BatchEntity;
import edu.jxust.agritrace.module.batch.entity.BatchRiskActionEntity;
import edu.jxust.agritrace.module.batch.entity.BatchStatus;
import edu.jxust.agritrace.module.batch.entity.CompanyEntity;
import edu.jxust.agritrace.module.batch.entity.FileAssetEntity;
import edu.jxust.agritrace.module.batch.entity.MasterDataStatus;
import edu.jxust.agritrace.module.batch.entity.ProductEntity;
import edu.jxust.agritrace.module.batch.entity.QrCodeEntity;
import edu.jxust.agritrace.module.batch.entity.QualityReportEntity;
import edu.jxust.agritrace.module.batch.entity.RiskActionType;
import edu.jxust.agritrace.module.batch.entity.ScanRecordEntity;
import edu.jxust.agritrace.module.batch.entity.StatusHistoryEntity;
import edu.jxust.agritrace.module.batch.entity.TraceRecordEntity;
import edu.jxust.agritrace.module.batch.entity.TraceStage;
import edu.jxust.agritrace.module.batch.mapper.BaseProductMapper;
import edu.jxust.agritrace.module.batch.mapper.BatchFieldDraftMapper;
import edu.jxust.agritrace.module.batch.mapper.BatchRiskActionMapper;
import edu.jxust.agritrace.module.batch.mapper.BatchStatusLogMapper;
import edu.jxust.agritrace.module.batch.mapper.BizAttachmentMapper;
import edu.jxust.agritrace.module.batch.mapper.OrgCompanyMapper;
import edu.jxust.agritrace.module.batch.mapper.QrCodeMapper;
import edu.jxust.agritrace.module.batch.mapper.QrQueryLogMapper;
import edu.jxust.agritrace.module.batch.mapper.QualityReportMapper;
import edu.jxust.agritrace.module.batch.mapper.TraceBatchMapper;
import edu.jxust.agritrace.module.batch.mapper.TraceEventMapper;
import edu.jxust.agritrace.module.batch.mapper.po.BaseProductPO;
import edu.jxust.agritrace.module.batch.mapper.po.BatchFieldDraftPO;
import edu.jxust.agritrace.module.batch.mapper.po.BatchRiskActionPO;
import edu.jxust.agritrace.module.batch.mapper.po.BatchStatusLogPO;
import edu.jxust.agritrace.module.batch.mapper.po.BizAttachmentPO;
import edu.jxust.agritrace.module.batch.mapper.po.OrgCompanyPO;
import edu.jxust.agritrace.module.batch.mapper.po.QrCodePO;
import edu.jxust.agritrace.module.batch.mapper.po.QrQueryLogPO;
import edu.jxust.agritrace.module.batch.mapper.po.QualityReportPO;
import edu.jxust.agritrace.module.batch.mapper.po.TraceBatchPO;
import edu.jxust.agritrace.module.batch.mapper.po.TraceEventPO;
import edu.jxust.agritrace.module.batch.service.BatchService;
import edu.jxust.agritrace.module.batch.service.MasterDataService;
import edu.jxust.agritrace.module.batch.service.support.AttachmentGovernanceService;
import edu.jxust.agritrace.module.batch.service.support.AttachmentStorageService;
import edu.jxust.agritrace.module.batch.service.support.BatchRiskResolver;
import edu.jxust.agritrace.module.batch.service.support.BatchStatusFlowAdvisor;
import edu.jxust.agritrace.module.batch.service.support.TraceDisplayLabels;
import edu.jxust.agritrace.module.batch.service.support.QrImageStorageService;
import edu.jxust.agritrace.module.batch.service.support.TraceLinkBuilder;
import edu.jxust.agritrace.module.batch.vo.AttachmentCleanupResultVO;
import edu.jxust.agritrace.module.batch.vo.BatchActionVO;
import edu.jxust.agritrace.module.batch.vo.BatchListItemVO;
import edu.jxust.agritrace.module.batch.vo.BatchOverviewVO;
import edu.jxust.agritrace.module.batch.vo.BatchRiskActionVO;
import edu.jxust.agritrace.module.batch.vo.BatchRiskSummaryVO;
import edu.jxust.agritrace.module.batch.vo.BatchStatusLogVO;
import edu.jxust.agritrace.module.batch.vo.BatchStatusSummaryVO;
import edu.jxust.agritrace.module.batch.vo.BatchTaskSummaryVO;
import edu.jxust.agritrace.module.batch.vo.BatchWorkbenchVO;
import edu.jxust.agritrace.module.batch.vo.CompanyOptionVO;
import edu.jxust.agritrace.module.batch.vo.CompanySummaryVO;
import edu.jxust.agritrace.module.batch.vo.FileAssetVO;
import edu.jxust.agritrace.module.batch.vo.FieldDraftVO;
import edu.jxust.agritrace.module.batch.vo.OperatorOptionVO;
import edu.jxust.agritrace.module.batch.vo.ProductSummaryVO;
import edu.jxust.agritrace.module.batch.vo.ProductOptionVO;
import edu.jxust.agritrace.module.batch.vo.QrSummaryVO;
import edu.jxust.agritrace.module.batch.vo.QualityReportVO;
import edu.jxust.agritrace.module.batch.vo.QualitySectionVO;
import edu.jxust.agritrace.module.batch.vo.RiskHandlingSectionVO;
import edu.jxust.agritrace.module.batch.vo.ScanRecordVO;
import edu.jxust.agritrace.module.batch.vo.ScanStatsSectionVO;
import edu.jxust.agritrace.module.batch.vo.ScanTrendPointVO;
import edu.jxust.agritrace.module.batch.vo.TraceChainVerificationVO;
import edu.jxust.agritrace.module.batch.vo.TraceRecordVO;
import edu.jxust.agritrace.module.batch.vo.TraceSectionVO;
import edu.jxust.agritrace.module.log.dto.OperationLogRecord;
import edu.jxust.agritrace.module.log.service.OperationLogService;
import edu.jxust.agritrace.module.publictrace.dto.PublicTraceAccessContext;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HexFormat;

@Service
@Transactional(readOnly = true)
public class BatchServiceImpl implements BatchService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TRACE_HASH_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private final TraceBatchMapper traceBatchMapper;
    private final BaseProductMapper baseProductMapper;
    private final OrgCompanyMapper orgCompanyMapper;
    private final SysUserMapper sysUserMapper;
    private final TraceEventMapper traceEventMapper;
    private final QualityReportMapper qualityReportMapper;
    private final BizAttachmentMapper bizAttachmentMapper;
    private final BatchFieldDraftMapper batchFieldDraftMapper;
    private final BatchRiskActionMapper batchRiskActionMapper;
    private final QrCodeMapper qrCodeMapper;
    private final BatchStatusLogMapper batchStatusLogMapper;
    private final QrQueryLogMapper qrQueryLogMapper;
    private final ObjectMapper objectMapper;
    private final MasterDataService masterDataService;
    private final TraceLinkBuilder traceLinkBuilder;
    private final QrImageStorageService qrImageStorageService;
    private final AttachmentStorageService attachmentStorageService;
    private final AttachmentGovernanceService attachmentGovernanceService;
    private final BatchRiskResolver batchRiskResolver;
    private final BatchStatusFlowAdvisor batchStatusFlowAdvisor;
    private final OperationLogService operationLogService;

    public BatchServiceImpl(
            TraceBatchMapper traceBatchMapper,
            BaseProductMapper baseProductMapper,
            OrgCompanyMapper orgCompanyMapper,
            SysUserMapper sysUserMapper,
            TraceEventMapper traceEventMapper,
            QualityReportMapper qualityReportMapper,
            BizAttachmentMapper bizAttachmentMapper,
            BatchFieldDraftMapper batchFieldDraftMapper,
            BatchRiskActionMapper batchRiskActionMapper,
            QrCodeMapper qrCodeMapper,
            BatchStatusLogMapper batchStatusLogMapper,
            QrQueryLogMapper qrQueryLogMapper,
            ObjectMapper objectMapper,
            MasterDataService masterDataService,
            TraceLinkBuilder traceLinkBuilder,
            QrImageStorageService qrImageStorageService,
            AttachmentStorageService attachmentStorageService,
            AttachmentGovernanceService attachmentGovernanceService,
            BatchRiskResolver batchRiskResolver,
            BatchStatusFlowAdvisor batchStatusFlowAdvisor,
            OperationLogService operationLogService
    ) {
        this.traceBatchMapper = traceBatchMapper;
        this.baseProductMapper = baseProductMapper;
        this.orgCompanyMapper = orgCompanyMapper;
        this.sysUserMapper = sysUserMapper;
        this.traceEventMapper = traceEventMapper;
        this.qualityReportMapper = qualityReportMapper;
        this.bizAttachmentMapper = bizAttachmentMapper;
        this.batchFieldDraftMapper = batchFieldDraftMapper;
        this.batchRiskActionMapper = batchRiskActionMapper;
        this.qrCodeMapper = qrCodeMapper;
        this.batchStatusLogMapper = batchStatusLogMapper;
        this.qrQueryLogMapper = qrQueryLogMapper;
        this.objectMapper = objectMapper;
        this.masterDataService = masterDataService;
        this.traceLinkBuilder = traceLinkBuilder;
        this.qrImageStorageService = qrImageStorageService;
        this.attachmentStorageService = attachmentStorageService;
        this.attachmentGovernanceService = attachmentGovernanceService;
        this.batchRiskResolver = batchRiskResolver;
        this.batchStatusFlowAdvisor = batchStatusFlowAdvisor;
        this.operationLogService = operationLogService;
    }

    @Override
    public List<BatchListItemVO> listBatches(BatchListQueryRequest request) {
        AuthUserSession currentUser = currentUser();
        LambdaQueryWrapper<TraceBatchPO> wrapper = new LambdaQueryWrapper<TraceBatchPO>()
                .orderByDesc(TraceBatchPO::getId);
        applyBatchReadScope(wrapper, currentUser, Boolean.TRUE.equals(request == null ? null : request.getMineOnly()));
        if (request != null) {
            if (notBlank(request.getBatchCode())) {
                wrapper.like(TraceBatchPO::getBatchCode, request.getBatchCode().trim());
            }
            if (notBlank(request.getStatus())) {
                wrapper.eq(TraceBatchPO::getStatus, normalizeStatusForQuery(request.getStatus()));
            }
            LocalDate dateFrom = parseNullableDate(request.getDateFrom());
            LocalDate dateTo = parseNullableDate(request.getDateTo());
            if (dateFrom != null) {
                wrapper.ge(TraceBatchPO::getStartDate, dateFrom);
            }
            if (dateTo != null) {
                wrapper.le(TraceBatchPO::getStartDate, dateTo);
            }
        }

        return traceBatchMapper.selectList(wrapper).stream()
                .map(this::loadBatchEntity)
                .filter(batch -> matchesExtraFilters(batch, request))
                .map(this::toListItem)
                .toList();
    }

    @Override
    public BatchWorkbenchVO getBatchWorkbench(Long batchId) {
        return toWorkbench(getBatchEntityById(batchId));
    }

    @Override
    public List<CompanyOptionVO> listCompanyOptions(String keyword) {
        return masterDataService.listCompanyOptions(keyword);
    }

    @Override
    public List<ProductOptionVO> listProductOptions(Long companyId, String keyword) {
        return masterDataService.listProductOptions(companyId, keyword);
    }

    @Override
    public List<OperatorOptionVO> listAssignableOperators(Long companyId) {
        AuthUserSession currentUser = requireCurrentUser();
        ensureAssignmentManager(currentUser);

        Long effectiveCompanyId = normalizeOperatorLookupCompanyId(currentUser, companyId);
        List<SysUserPO> operators = sysUserMapper.selectList(new LambdaQueryWrapper<SysUserPO>()
                        .eq(SysUserPO::getStatus, 1)
                        .eq(SysUserPO::getRoleCode, "OPERATOR")
                        .eq(effectiveCompanyId != null, SysUserPO::getCompanyId, effectiveCompanyId)
                        .orderByAsc(SysUserPO::getCompanyId)
                        .orderByAsc(SysUserPO::getUsername))
                .stream()
                .filter(Objects::nonNull)
                .toList();

        Map<Long, String> companyNameMap = new LinkedHashMap<>();
        for (SysUserPO operator : operators) {
            if (operator.getCompanyId() == null || companyNameMap.containsKey(operator.getCompanyId())) {
                continue;
            }
            OrgCompanyPO companyPO = orgCompanyMapper.selectById(operator.getCompanyId());
            companyNameMap.put(operator.getCompanyId(), companyPO == null ? "" : defaultValue(companyPO.getName(), ""));
        }

        return operators.stream()
                .map(operator -> new OperatorOptionVO(
                        operator.getId(),
                        operator.getUsername(),
                        defaultValue(operator.getRealName(), defaultValue(operator.getUsername(), "")),
                        operator.getRoleCode(),
                        operator.getCompanyId(),
                        companyNameMap.getOrDefault(operator.getCompanyId(), "")
                ))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<FileAssetVO> uploadAttachments(String businessType, List<MultipartFile> files) {
        AttachmentBusinessType attachmentBusinessType = AttachmentBusinessType.fromCode(businessType);
        attachmentGovernanceService.cleanupExpiredOrphans();
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("files cannot be empty");
        }
        List<FileAssetVO> uploaded = new ArrayList<>();
        for (MultipartFile file : files) {
            AttachmentStorageService.StoredAttachment storedAttachment = attachmentStorageService.store(file, attachmentBusinessType);
            BizAttachmentPO attachmentPO = new BizAttachmentPO();
            attachmentPO.setFileName(storedAttachment.fileName());
            attachmentPO.setFilePath(storedAttachment.filePath());
            attachmentPO.setContentType(storedAttachment.contentType());
            attachmentPO.setSize(storedAttachment.size());
            attachmentPO.setBusinessType(attachmentBusinessType.code());
            attachmentPO.setBusinessId(null);
            bizAttachmentMapper.insert(attachmentPO);
            attachmentPO.setFileUrl(traceLinkBuilder.buildAttachmentUrl(attachmentPO.getId()));
            bizAttachmentMapper.updateById(attachmentPO);
            uploaded.add(toFileAssetVO(attachmentPO));
        }
        AuthUserSession currentUser = currentUser();
        if (attachmentBusinessType == AttachmentBusinessType.TRACE_IMAGE && currentUser != null && !uploaded.isEmpty()) {
            FileAssetVO firstFile = uploaded.get(0);
            writeOperationLog(
                    currentUser,
                    currentUser.companyId(),
                    "TRACE_IMAGE_UPLOAD",
                    "ATTACHMENT",
                    firstFile.id(),
                    "现场图片（" + uploaded.size() + "张）",
                    "上传现场图片成功，共 " + uploaded.size() + " 张"
            );
        }
        return uploaded;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttachmentCleanupResultVO cleanupExpiredOrphanAttachments() {
        return attachmentGovernanceService.cleanupExpiredOrphans();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchWorkbenchVO createBatch(BatchCreateRequest request) {
        AuthUserSession currentUser = requireCurrentUser();
        ensureBatchEditor(currentUser, request.companyId(), null, "创建批次");
        ensureBatchCodeUnique(request.batchCode(), null);
        OrgCompanyPO company = findCompanyRequired(request.companyId());
        BaseProductPO product = findProductRequired(request.productId());
        validateProductCompany(product, company.getId());

        TraceBatchPO batchPO = new TraceBatchPO();
        batchPO.setBatchCode(request.batchCode().trim());
        batchPO.setProductId(product.getId());
        batchPO.setCompanyId(company.getId());
        batchPO.setTaskStatus("PENDING");
        batchPO.setOriginPlace(request.originPlace().trim());
        batchPO.setStartDate(parseRequiredDate(request.productionDate(), "productionDate"));
        batchPO.setStatus(BatchStatus.DRAFT.name());
        batchPO.setPublicRemark(defaultValue(request.publicRemark(), "先补录关键环节、质检摘要和二维码，再对外发布。"));
        batchPO.setInternalRemark(defaultValue(request.internalRemark(), "当前是轻量批次草稿，优先保证主流程跑通。"));
        traceBatchMapper.insert(batchPO);

        writeStatusLog(batchPO.getId(), BatchStatus.DRAFT, "创建批次草稿", "企业管理员", LocalDateTime.now());
        return toWorkbench(getBatchEntityById(batchPO.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchWorkbenchVO updateBatch(Long batchId, BatchUpdateRequest request) {
        TraceBatchPO batchPO = findBatchPO(batchId);
        AuthUserSession currentUser = requireCurrentUser();
        ensureBatchEditor(currentUser, request.companyId(), batchPO, "编辑批次");
        OrgCompanyPO company = findCompanyRequired(request.companyId());
        BaseProductPO product = findProductRequired(request.productId());
        validateProductCompany(product, company.getId());

        batchPO.setProductId(product.getId());
        batchPO.setCompanyId(company.getId());
        batchPO.setOriginPlace(request.originPlace().trim());
        batchPO.setStartDate(parseRequiredDate(request.productionDate(), "productionDate"));
        batchPO.setPublicRemark(defaultValue(request.publicRemark(), batchPO.getPublicRemark()));
        batchPO.setInternalRemark(defaultValue(request.internalRemark(), batchPO.getInternalRemark()));
        traceBatchMapper.updateById(batchPO);

        return toWorkbench(getBatchEntityById(batchId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchWorkbenchVO changeStatus(Long batchId, BatchStatusActionRequest request) {
        TraceBatchPO batchPO = findBatchPO(batchId);
        AuthUserSession currentUser = requireCurrentUser();
        ensureStatusManager(currentUser, batchPO, request.targetStatus());
        BatchEntity batch = loadBatchEntity(batchPO);
        BatchStatus currentStatus = batch.getStatus();
        BatchStatus targetStatus = request.targetStatus();
        validateStatusTransition(batch, currentStatus, targetStatus);

        LocalDateTime operatedAt = LocalDateTime.now();
        batchPO.setStatus(targetStatus.name());
        batchPO.setStatusReason(request.reason());
        if (targetStatus == BatchStatus.PUBLISHED && batchPO.getPublishedAt() == null) {
            batchPO.setPublishedAt(operatedAt);
        }
        if (targetStatus == BatchStatus.FROZEN) {
            batchPO.setFrozenAt(operatedAt);
        }
        if (targetStatus == BatchStatus.RECALLED) {
            batchPO.setRecalledAt(operatedAt);
        }
        traceBatchMapper.updateById(batchPO);
        writeStatusLog(batchId, targetStatus, request.reason(), request.operatorName(), operatedAt);

        QrCodePO qrCodePO = findQrByBatchId(batchId);
        if (qrCodePO != null) {
            qrCodePO.setStatus(toQrStatus(targetStatus));
            qrCodePO.setStatusReason(request.reason());
            qrCodeMapper.updateById(qrCodePO);
        }

        String actionType = null;
        String summary = null;
        if (targetStatus == BatchStatus.FROZEN) {
            actionType = "RISK_FREEZE";
            summary = "冻结批次 " + batchPO.getBatchCode() + "，原因：" + defaultValue(request.reason(), "已进入风险处理");
        } else if (targetStatus == BatchStatus.PUBLISHED) {
            if (currentStatus == BatchStatus.FROZEN || currentStatus == BatchStatus.RECALLED) {
                actionType = "RISK_RESUME_PUBLISH";
                summary = "恢复批次 " + batchPO.getBatchCode() + " 发布，说明：" + defaultValue(request.reason(), "整改完成");
            } else {
                actionType = "BATCH_PUBLISH";
                summary = "发布批次 " + batchPO.getBatchCode() + " 到公开页";
            }
        }
        if (actionType != null) {
            writeOperationLog(currentUser, batchPO.getCompanyId(), actionType, "BATCH", batchId, batchPO.getBatchCode(), summary);
        }

        return toWorkbench(getBatchEntityById(batchId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchWorkbenchVO addTraceRecord(Long batchId, TraceRecordCreateRequest request) {
        TraceBatchPO batchPO = findBatchPO(batchId);
        AuthUserSession currentUser = requireCurrentUser();
        ensureTraceWriter(currentUser, batchPO);
        LocalDateTime eventTime = parseFlexibleDateTime(request.eventTime(), LocalDateTime.now());
        TraceStage stage = request.stage() == null ? TraceStage.PRODUCE : request.stage();
        backfillMissingTraceHashes(batchId);
        TraceEventPO previousEvent = findLatestTraceEvent(batchId);
        String previousHash = previousEvent == null ? null : normalizeHash(previousEvent.getHash());
        List<BizAttachmentPO> attachments = claimAttachments(request.attachmentIds(), AttachmentBusinessType.TRACE_IMAGE, null);
        String resolvedImageUrl = firstAttachmentUrl(attachments, request.imageUrl());
        String contentJson = writeTraceContent(request.summary(), resolvedImageUrl);
        String attachmentsJson = writeAttachments(attachments, resolvedImageUrl);

        TraceEventPO eventPO = new TraceEventPO();
        eventPO.setBatchId(batchId);
        eventPO.setCompanyId(batchPO.getCompanyId());
        eventPO.setStage(stage.name());
        eventPO.setTitle(notBlank(request.title()) ? request.title().trim() : stage.label() + "补录");
        eventPO.setEventTime(eventTime);
        eventPO.setOperatorName(request.operatorName().trim());
        eventPO.setLocation(request.location().trim());
        eventPO.setIsPublic(request.visibleToConsumer());
        eventPO.setContentJson(contentJson);
        eventPO.setAttachmentsJson(attachmentsJson);
        eventPO.setPrevHash(previousHash);
        eventPO.setHash(buildTraceHash(eventPO, previousHash));
        traceEventMapper.insert(eventPO);
        bindAttachmentsToBusiness(attachments, eventPO.getId());
        markTaskCompleted(batchPO, currentUser);
        deleteFieldDraftRecord(batchId, currentUser);
        writeOperationLog(
                currentUser,
                batchPO.getCompanyId(),
                "TRACE_RECORD_SUBMIT",
                "TRACE_RECORD",
                eventPO.getId(),
                eventPO.getTitle(),
                "为批次 " + batchPO.getBatchCode() + " 提交现场记录《" + eventPO.getTitle() + "》"
        );

        return toWorkbench(getBatchEntityById(batchId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TraceChainVerificationVO verifyTraceChain(Long batchId) {
        findBatchPO(batchId);
        backfillMissingTraceHashes(batchId);
        List<TraceEventPO> events = listTraceEvents(batchId);
        LocalDateTime checkedAt = LocalDateTime.now();

        if (events.isEmpty()) {
            return new TraceChainVerificationVO(
                    true,
                    "校验通过",
                    "当前批次暂无追溯记录，首条记录写入后会自动生成 Hash 链。",
                    0,
                    null,
                    null,
                    formatDateTime(checkedAt)
            );
        }

        String previousHash = null;
        for (int index = 0; index < events.size(); index++) {
            TraceEventPO event = events.get(index);
            String storedPrevHash = normalizeHash(event.getPrevHash());
            String storedHash = normalizeHash(event.getHash());

            if (!Objects.equals(blankToEmpty(storedPrevHash), blankToEmpty(previousHash))) {
                return new TraceChainVerificationVO(
                        false,
                        "校验失败",
                        "第 " + (index + 1) + " 条记录的前序哈希与上一条记录不一致。",
                        events.size(),
                        event.getId(),
                        normalizeHash(events.get(events.size() - 1).getHash()),
                        formatDateTime(checkedAt)
                );
            }

            String expectedHash = buildTraceHash(event, previousHash);
            if (!Objects.equals(storedHash, expectedHash)) {
                return new TraceChainVerificationVO(
                        false,
                        "校验失败",
                        "第 " + (index + 1) + " 条记录的内容摘要与存储哈希不一致，记录可能已被改动。",
                        events.size(),
                        event.getId(),
                        normalizeHash(events.get(events.size() - 1).getHash()),
                        formatDateTime(checkedAt)
                );
            }

            previousHash = storedHash;
        }

        return new TraceChainVerificationVO(
                true,
                "校验通过",
                "已按时间顺序完成 " + events.size() + " 条追溯记录的 Hash 链校验。",
                events.size(),
                null,
                previousHash,
                formatDateTime(checkedAt)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchWorkbenchVO assignBatchOperator(Long batchId, BatchAssignmentRequest request) {
        TraceBatchPO batchPO = findBatchPO(batchId);
        AuthUserSession currentUser = requireCurrentUser();
        ensureAssignmentManagerForBatch(currentUser, batchPO);

        Long currentAssigneeUserId = batchPO.getAssigneeUserId();
        Long nextAssigneeUserId = request == null ? null : request.assigneeUserId();
        boolean forceClearDraft = request != null && Boolean.TRUE.equals(request.forceClearDraft());
        boolean assignmentChanging = !Objects.equals(batchPO.getAssigneeUserId(), nextAssigneeUserId);
        BatchFieldDraftPO existingDraft = findFieldDraftPO(batchId, currentAssigneeUserId);
        SysUserPO previousAssigneePO = currentAssigneeUserId == null ? null : sysUserMapper.selectById(currentAssigneeUserId);
        SysUserPO nextAssigneePO = null;

        if (!assignmentChanging) {
            return toWorkbench(getBatchEntityById(batchId));
        }
        if (existingDraft != null && !forceClearDraft) {
            throw new IllegalArgumentException("该批次当前分配人存在未提交草稿，请先取消改派，或确认强制改派并清除原分配人的未提交草稿。");
        }

        if (nextAssigneeUserId == null) {
            batchPO.setAssigneeUserId(null);
            batchPO.setAssignedAt(null);
        } else {
            nextAssigneePO = findAssignableOperatorRequired(nextAssigneeUserId, batchPO, currentUser);
            batchPO.setAssigneeUserId(nextAssigneePO.getId());
            batchPO.setAssignedAt(LocalDateTime.now());
        }
        batchPO.setTaskStatus("PENDING");
        batchPO.setTaskCompletedAt(null);
        traceBatchMapper.updateById(batchPO);

        if (existingDraft != null && currentAssigneeUserId != null) {
            deleteFieldDraftRecord(batchId, currentAssigneeUserId);
        }

        writeOperationLog(
                currentUser,
                batchPO.getCompanyId(),
                resolveAssignmentActionType(previousAssigneePO, nextAssigneePO),
                "BATCH",
                batchId,
                batchPO.getBatchCode(),
                buildAssignmentSummary(batchPO.getBatchCode(), previousAssigneePO, nextAssigneePO, existingDraft != null && forceClearDraft)
        );

        return toWorkbench(getBatchEntityById(batchId));
    }

    @Override
    public List<FieldDraftVO> listMyFieldDrafts() {
        AuthUserSession currentUser = requireCurrentUser();
        return batchFieldDraftMapper.selectList(new LambdaQueryWrapper<BatchFieldDraftPO>()
                        .eq(BatchFieldDraftPO::getOperatorUserId, currentUser.userId())
                        .orderByDesc(BatchFieldDraftPO::getUpdatedAt)
                        .orderByDesc(BatchFieldDraftPO::getId))
                .stream()
                .map(this::toFieldDraftVO)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public FieldDraftVO getMyFieldDraft(Long batchId) {
        TraceBatchPO batchPO = findBatchPO(batchId);
        return toFieldDraftVO(findFieldDraftPO(batchId, requireCurrentUser()), batchPO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FieldDraftVO saveFieldDraft(Long batchId, FieldDraftSaveRequest request) {
        TraceBatchPO batchPO = findBatchPO(batchId);
        AuthUserSession currentUser = requireCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        BatchFieldDraftPO draftPO = findFieldDraftPO(batchId, currentUser);
        if (draftPO == null) {
            draftPO = new BatchFieldDraftPO();
            draftPO.setBatchId(batchId);
            draftPO.setOperatorUserId(currentUser.userId());
            draftPO.setCreatedAt(now);
        }
        draftPO.setStage(normalizeDraftStage(request == null ? null : request.stage()));
        draftPO.setTitle(trimToNull(request == null ? null : request.title()));
        draftPO.setEventTime(parseFlexibleDateTime(request == null ? null : request.eventTime(), now));
        draftPO.setOperatorName(defaultValue(trimToNull(request == null ? null : request.operatorName()), defaultOperatorName(currentUser)));
        draftPO.setLocation(defaultValue(trimToNull(request == null ? null : request.location()), batchPO.getOriginPlace()));
        draftPO.setSummary(defaultValue(trimToNull(request == null ? null : request.summary()), ""));
        draftPO.setImageUrl(trimToNull(request == null ? null : request.imageUrl()));
        draftPO.setAttachmentIdsJson(writeJson(sanitizeAttachmentIds(request == null ? null : request.attachmentIds())));
        draftPO.setUploadedFilesJson(writeJson(sanitizeDraftFiles(request == null ? null : request.uploadedFiles())));
        draftPO.setVisibleToConsumer(request == null || request.visibleToConsumer() == null ? Boolean.TRUE : request.visibleToConsumer());
        draftPO.setUpdatedAt(now);
        if (draftPO.getId() == null) {
            batchFieldDraftMapper.insert(draftPO);
        } else {
            batchFieldDraftMapper.updateById(draftPO);
        }
        markTaskDrafting(batchPO);
        return toFieldDraftVO(draftPO, batchPO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFieldDraft(Long batchId) {
        TraceBatchPO batchPO = findBatchPO(batchId);
        AuthUserSession currentUser = requireCurrentUser();
        deleteFieldDraftRecord(batchId, currentUser);
        markTaskPendingIfNeeded(batchPO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchWorkbenchVO addQualityReport(Long batchId, QualityReportCreateRequest request) {
        TraceBatchPO batchPO = findBatchPO(batchId);
        AuthUserSession currentUser = requireCurrentUser();
        ensureQualityWriter(currentUser, batchPO);
        LocalDateTime reportTime = parseFlexibleDateTime(request.reportTime(), LocalDateTime.now());
        List<String> highlights = sanitizeQualityHighlights(request.highlights());
        if (highlights.isEmpty()) {
            throw new IllegalArgumentException("quality highlights cannot be empty");
        }
        List<BizAttachmentPO> attachments = claimAttachments(request.attachmentIds(), AttachmentBusinessType.QUALITY_ATTACHMENT, null);

        QualityReportPO reportPO = new QualityReportPO();
        reportPO.setBatchId(batchId);
        reportPO.setReportNo(request.reportNo().trim());
        reportPO.setAgency(request.agency().trim());
        reportPO.setResult(request.result().trim().toUpperCase(Locale.ROOT));
        reportPO.setReportFileUrl(firstAttachmentUrl(attachments, null));
        reportPO.setReportJson(writeQualityJson(highlights, attachments));
        reportPO.setCreatedAt(reportTime);
        qualityReportMapper.insert(reportPO);
        bindAttachmentsToBusiness(attachments, reportPO.getId());
        writeOperationLog(
                currentUser,
                batchPO.getCompanyId(),
                "QUALITY_UPLOAD",
                "QUALITY",
                reportPO.getId(),
                reportPO.getReportNo(),
                "为批次 " + batchPO.getBatchCode() + " 上传质检，报告号：" + reportPO.getReportNo() + "，结论：" + TraceDisplayLabels.qualityStatus(reportPO.getResult())
        );

        return toWorkbench(getBatchEntityById(batchId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchWorkbenchVO addRiskAction(Long batchId, BatchRiskActionCreateRequest request) {
        TraceBatchPO batchPO = findBatchPO(batchId);
        AuthUserSession currentUser = requireCurrentUser();
        ensureRiskWriter(currentUser, batchPO);
        BatchEntity batch = loadBatchEntity(batchPO);
        if (batch.getStatus() != BatchStatus.FROZEN && batch.getStatus() != BatchStatus.RECALLED) {
            throw new IllegalArgumentException("risk handling can only be added when the batch is frozen or recalled");
        }
        validateRiskActionRequest(request);
        validateRiskActionFlow(batch, request);

        BatchRiskActionPO actionPO = new BatchRiskActionPO();
        actionPO.setBatchId(batchId);
        actionPO.setActionType(request.actionType().code());
        actionPO.setReason(trimToNull(request.reason()));
        actionPO.setComment(trimToNull(request.comment()));
        actionPO.setOperatorName(request.operatorName().trim());
        actionPO.setCreatedAt(LocalDateTime.now());
        batchRiskActionMapper.insert(actionPO);

        if (request.actionType() == RiskActionType.PROCESSING || request.actionType() == RiskActionType.RECTIFIED) {
            batchPO.setStatusReason(defaultValue(request.reason(), defaultValue(request.comment(), batchPO.getStatusReason())));
            traceBatchMapper.updateById(batchPO);
        }
        writeOperationLog(
                currentUser,
                batchPO.getCompanyId(),
                resolveRiskActionLogType(request.actionType()),
                "BATCH",
                batchId,
                batchPO.getBatchCode(),
                buildRiskActionSummary(batchPO.getBatchCode(), request)
        );

        return toWorkbench(getBatchEntityById(batchId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchWorkbenchVO generateQr(Long batchId) {
        TraceBatchPO batchPO = findBatchPO(batchId);
        AuthUserSession currentUser = requireCurrentUser();
        ensureQrPublisher(currentUser, batchPO, "生成二维码");
        QrCodePO existing = findQrByBatchId(batchId);
        QrCodePO effectiveQrCode;
        if (existing == null) {
            String token = resolveUniqueToken(batchPO.getBatchCode(), batchId);
            String publicUrl = traceLinkBuilder.buildPublicTraceUrl(token);
            qrImageStorageService.ensureQrImage(token, publicUrl);

            QrCodePO qrCodePO = new QrCodePO();
            qrCodePO.setBatchId(batchId);
            qrCodePO.setQrToken(token);
            qrCodePO.setStatus(toQrStatus(readBatchStatus(batchPO.getStatus())));
            qrCodePO.setCreatedAt(LocalDateTime.now());
            qrCodePO.setRemark("批次二维码");
            qrCodePO.setPv(0L);
            qrCodeMapper.insert(qrCodePO);
            effectiveQrCode = qrCodePO;
        } else {
            qrImageStorageService.ensureQrImage(existing.getQrToken(), traceLinkBuilder.buildPublicTraceUrl(existing.getQrToken()));
            effectiveQrCode = existing;
        }
        writeOperationLog(
                currentUser,
                batchPO.getCompanyId(),
                "QR_GENERATE",
                "QR",
                effectiveQrCode.getId(),
                effectiveQrCode.getQrToken(),
                "为批次 " + batchPO.getBatchCode() + " 生成二维码，公开标识：" + effectiveQrCode.getQrToken()
        );
        return toWorkbench(getBatchEntityById(batchId));
    }

    @Override
    public BatchEntity getBatchEntityById(Long batchId) {
        return loadBatchEntity(findBatchPO(batchId));
    }

    @Override
    public BatchEntity getBatchEntityByToken(String token) {
        QrCodePO qrCodePO = findQrByToken(token);
        return loadBatchEntity(findBatchPO(qrCodePO.getBatchId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordPublicTraceAccess(String token, PublicTraceAccessContext accessContext) {
        QrCodePO qrCodePO = findQrByToken(token);
        LocalDateTime accessTime = LocalDateTime.now();

        QrQueryLogPO queryLogPO = new QrQueryLogPO();
        queryLogPO.setQrId(qrCodePO.getId());
        queryLogPO.setBatchId(qrCodePO.getBatchId());
        queryLogPO.setQueryTime(accessTime);
        queryLogPO.setIp(trimToNull(accessContext == null ? null : accessContext.ip()));
        queryLogPO.setUa(trimToNull(accessContext == null ? null : accessContext.userAgent()));
        queryLogPO.setReferer(trimToNull(accessContext == null ? null : accessContext.referer()));
        qrQueryLogMapper.insert(queryLogPO);

        qrCodePO.setLastQueryAt(accessTime);
        qrCodePO.setPv(defaultLong(qrCodePO.getPv()) + 1);
        qrCodeMapper.updateById(qrCodePO);
    }

    @Override
    public Resource loadQrImage(String token) {
        QrCodePO qrCodePO = findQrByToken(token);
        qrImageStorageService.ensureQrImage(qrCodePO.getQrToken(), traceLinkBuilder.buildPublicTraceUrl(qrCodePO.getQrToken()));
        return qrImageStorageService.loadQrImage(qrCodePO.getQrToken());
    }

    @Override
    public Resource loadAttachment(Long fileId) {
        BizAttachmentPO attachmentPO = findAttachmentRequired(fileId);
        return attachmentStorageService.load(attachmentPO.getFilePath());
    }

    private TraceBatchPO findBatchPO(Long batchId) {
        TraceBatchPO batchPO = traceBatchMapper.selectById(batchId);
        if (batchPO == null) {
            throw new IllegalArgumentException("batch not found");
        }
        ensureBatchAccessible(batchPO);
        return batchPO;
    }

    private QrCodePO findQrByBatchId(Long batchId) {
        return qrCodeMapper.selectOne(new LambdaQueryWrapper<QrCodePO>()
                .eq(QrCodePO::getBatchId, batchId)
                .orderByDesc(QrCodePO::getId)
                .last("limit 1"));
    }

    private QrCodePO findQrByToken(String token) {
        QrCodePO qrCodePO = qrCodeMapper.selectOne(new LambdaQueryWrapper<QrCodePO>()
                .eq(QrCodePO::getQrToken, token)
                .last("limit 1"));
        if (qrCodePO == null) {
            throw new IllegalArgumentException("未找到对应追溯码");
        }
        return qrCodePO;
    }

    private TraceEventPO findLatestTraceEvent(Long batchId) {
        return traceEventMapper.selectOne(new LambdaQueryWrapper<TraceEventPO>()
                .eq(TraceEventPO::getBatchId, batchId)
                .orderByDesc(TraceEventPO::getEventTime)
                .orderByDesc(TraceEventPO::getId)
                .last("limit 1"));
    }

    private List<TraceEventPO> listTraceEvents(Long batchId) {
        return traceEventMapper.selectList(new LambdaQueryWrapper<TraceEventPO>()
                .eq(TraceEventPO::getBatchId, batchId)
                .orderByAsc(TraceEventPO::getEventTime)
                .orderByAsc(TraceEventPO::getId));
    }

    private void backfillMissingTraceHashes(Long batchId) {
        String previousHash = null;
        for (TraceEventPO event : listTraceEvents(batchId)) {
            String storedPrevHash = normalizeHash(event.getPrevHash());
            String storedHash = normalizeHash(event.getHash());
            boolean changed = false;

            if (storedPrevHash == null && previousHash != null && storedHash != null) {
                String expectedHash = buildTraceHash(event, previousHash);
                if (Objects.equals(storedHash, expectedHash)) {
                    storedPrevHash = previousHash;
                    event.setPrevHash(previousHash);
                    changed = true;
                }
            }

            if (storedPrevHash == null && storedHash == null) {
                storedPrevHash = previousHash;
                storedHash = buildTraceHash(event, previousHash);
                event.setPrevHash(previousHash);
                event.setHash(storedHash);
                changed = true;
            } else if (storedHash == null) {
                storedHash = buildTraceHash(event, storedPrevHash);
                event.setHash(storedHash);
                changed = true;
            }

            if (changed) {
                traceEventMapper.updateById(event);
            }

            previousHash = storedHash;
        }
    }

    private BatchEntity loadBatchEntity(TraceBatchPO batchPO) {
        BaseProductPO productPO = baseProductMapper.selectById(batchPO.getProductId());
        OrgCompanyPO companyPO = orgCompanyMapper.selectById(batchPO.getCompanyId());
        SysUserPO assigneePO = batchPO.getAssigneeUserId() == null ? null : sysUserMapper.selectById(batchPO.getAssigneeUserId());
        if (productPO == null || companyPO == null) {
            throw new IllegalArgumentException("批次关联的产品或企业信息缺失");
        }

        List<TraceRecordEntity> traceRecords = listTraceEvents(batchPO.getId())
                .stream()
                .map(this::toTraceRecordEntity)
                .toList();

        List<QualityReportEntity> qualityReports = qualityReportMapper.selectList(new LambdaQueryWrapper<QualityReportPO>()
                        .eq(QualityReportPO::getBatchId, batchPO.getId())
                        .orderByDesc(QualityReportPO::getCreatedAt)
                        .orderByDesc(QualityReportPO::getId))
                .stream()
                .map(this::toQualityReportEntity)
                .toList();

        List<StatusHistoryEntity> statusHistory = batchStatusLogMapper.selectList(new LambdaQueryWrapper<BatchStatusLogPO>()
                        .eq(BatchStatusLogPO::getBatchId, batchPO.getId())
                        .orderByDesc(BatchStatusLogPO::getOperatedAt)
                        .orderByDesc(BatchStatusLogPO::getId))
                .stream()
                .map(this::toStatusHistoryEntity)
                .toList();

        List<BatchRiskActionEntity> riskActions = batchRiskActionMapper.selectList(new LambdaQueryWrapper<BatchRiskActionPO>()
                        .eq(BatchRiskActionPO::getBatchId, batchPO.getId())
                        .orderByDesc(BatchRiskActionPO::getCreatedAt)
                        .orderByDesc(BatchRiskActionPO::getId))
                .stream()
                .map(this::toBatchRiskActionEntity)
                .toList();

        QrCodePO qrCodePO = findQrByBatchId(batchPO.getId());
        List<QrQueryLogPO> qrLogs = qrCodePO == null
                ? List.of()
                : qrQueryLogMapper.selectList(new LambdaQueryWrapper<QrQueryLogPO>()
                .eq(QrQueryLogPO::getQrId, qrCodePO.getId())
                .orderByDesc(QrQueryLogPO::getQueryTime)
                .orderByDesc(QrQueryLogPO::getId));

        BatchEntity batch = new BatchEntity();
        batch.setId(batchPO.getId());
        batch.setBatchCode(batchPO.getBatchCode());
        batch.setProduct(toProductEntity(productPO));
        batch.setCompany(toCompanyEntity(companyPO));
        batch.setAssigneeUserId(batchPO.getAssigneeUserId());
        batch.setAssigneeName(resolveAssigneeName(assigneePO));
        batch.setAssignedAt(batchPO.getAssignedAt());
        batch.setTaskStatus(defaultValue(batchPO.getTaskStatus(), "PENDING"));
        batch.setTaskCompletedAt(batchPO.getTaskCompletedAt());
        batch.setOriginPlace(batchPO.getOriginPlace());
        batch.setProductionDate(batchPO.getStartDate());
        batch.setStatus(readBatchStatus(batchPO.getStatus()));
        batch.setStatusReason(batchPO.getStatusReason());
        batch.setPublicRemark(batchPO.getPublicRemark());
        batch.setInternalRemark(batchPO.getInternalRemark());
        batch.setPublishedAt(batchPO.getPublishedAt());
        batch.setFrozenAt(batchPO.getFrozenAt());
        batch.setRecalledAt(batchPO.getRecalledAt());
        batch.getTraceRecords().addAll(traceRecords);
        batch.getQualityReports().addAll(qualityReports);
        batch.getStatusHistory().addAll(statusHistory);
        batch.getRiskActions().addAll(riskActions);
        batch.getScanRecords().addAll(qrLogs.stream().map(this::toScanRecordEntity).toList());
        batch.setCurrentNode(resolveCurrentNode(batch, traceRecords));
        batch.setQrCode(toQrCodeEntity(qrCodePO, qrLogs));
        return batch;
    }

    private ProductEntity toProductEntity(BaseProductPO productPO) {
        return new ProductEntity(
                productPO.getId(),
                productPO.getCompanyId(),
                productPO.getName(),
                productPO.getCategory(),
                defaultValue(productPO.getSpec(), "待补充"),
                defaultValue(productPO.getUnit(), "待补充"),
                defaultValue(productPO.getImageUrl(), resolveProductImage(productPO.getName(), productPO.getCategory()))
        );
    }

    private CompanyEntity toCompanyEntity(OrgCompanyPO companyPO) {
        return new CompanyEntity(
                companyPO.getId(),
                companyPO.getName(),
                defaultValue(companyPO.getLicenseNo(), "待补充"),
                defaultValue(companyPO.getContact(), "待补充"),
                defaultValue(companyPO.getPhone(), "待补充"),
                defaultValue(companyPO.getAddress(), "待补充")
        );
    }

    private TraceRecordEntity toTraceRecordEntity(TraceEventPO eventPO) {
        List<FileAssetEntity> attachments = readAttachmentAssets(
                eventPO.getAttachmentsJson(),
                null,
                AttachmentBusinessType.TRACE_IMAGE.code(),
                eventPO.getId()
        );
        return new TraceRecordEntity(
                eventPO.getId(),
                TraceStage.fromCode(eventPO.getStage()),
                defaultValue(eventPO.getTitle(), TraceStage.fromCode(eventPO.getStage()).label()),
                eventPO.getEventTime(),
                defaultValue(eventPO.getOperatorName(), "未知操作人"),
                defaultValue(eventPO.getLocation(), "未填写地点"),
                Boolean.TRUE.equals(eventPO.getIsPublic()),
                readTraceSummary(eventPO.getContentJson()),
                readTraceImage(eventPO.getAttachmentsJson(), eventPO.getContentJson()),
                attachments
        );
    }

    private QualityReportEntity toQualityReportEntity(QualityReportPO reportPO) {
        return new QualityReportEntity(
                reportPO.getId(),
                reportPO.getReportNo(),
                defaultValue(reportPO.getAgency(), "未填写检测机构"),
                defaultValue(reportPO.getResult(), "REVIEW"),
                reportPO.getCreatedAt(),
                readQualityHighlights(reportPO.getReportJson()),
                readQualityAttachments(reportPO)
        );
    }

    private ScanRecordEntity toScanRecordEntity(QrQueryLogPO logPO) {
        return new ScanRecordEntity(
                logPO.getQueryTime(),
                defaultValue(logPO.getIp(), "unknown"),
                defaultValue(logPO.getUa(), "unknown"),
                defaultValue(logPO.getReferer(), "")
        );
    }

    private StatusHistoryEntity toStatusHistoryEntity(BatchStatusLogPO logPO) {
        return new StatusHistoryEntity(
                readBatchStatus(logPO.getStatus()),
                defaultValue(logPO.getReason(), "未填写原因"),
                defaultValue(logPO.getOperatorName(), "未知操作人"),
                logPO.getOperatedAt()
        );
    }

    private BatchRiskActionEntity toBatchRiskActionEntity(BatchRiskActionPO actionPO) {
        return new BatchRiskActionEntity(
                actionPO.getId(),
                RiskActionType.fromCode(actionPO.getActionType()),
                actionPO.getReason(),
                actionPO.getComment(),
                defaultValue(actionPO.getOperatorName(), "Unknown"),
                actionPO.getCreatedAt()
        );
    }

    private QrCodeEntity toQrCodeEntity(QrCodePO qrCodePO, List<QrQueryLogPO> logs) {
        if (qrCodePO == null) {
            return null;
        }
        long uv = logs.stream()
                .map(this::visitorKey)
                .filter(this::notBlank)
                .distinct()
                .count();
        long pv = Math.max(defaultLong(qrCodePO.getPv()), logs.size());
        return new QrCodeEntity(
                qrCodePO.getId(),
                qrCodePO.getQrToken(),
                defaultValue(qrCodePO.getStatus(), "READY"),
                traceLinkBuilder.buildPublicTraceUrl(qrCodePO.getQrToken()),
                traceLinkBuilder.buildQrImageUrl(qrCodePO.getQrToken()),
                qrCodePO.getCreatedAt(),
                qrCodePO.getLastQueryAt(),
                pv,
                uv
        );
    }

    private BatchListItemVO toListItem(BatchEntity batch) {
        QualityReportEntity latestQuality = latestQuality(batch);
        TraceRecordEntity latestTrace = latestTrace(batch);
        AuthUserSession currentUser = currentUser();
        String effectiveTaskStatus = resolveEffectiveTaskStatus(batch);
        LocalDateTime effectiveTaskCompletedAt = resolveEffectiveTaskCompletedAt(batch);
        BatchFieldDraftPO assignedDraft = findFieldDraftPO(batch.getId(), batch.getAssigneeUserId());
        String qrStatus = batch.getQrCode() == null ? "NOT_GENERATED" : batch.getQrCode().status();
        String qualityStatusCode = latestQuality == null
                ? "PENDING"
                : defaultValue(latestQuality.result(), "PENDING").toUpperCase(Locale.ROOT);
        String qualityStatusLabel = TraceDisplayLabels.qualityStatus(qualityStatusCode);
        BatchRiskResolver.RiskSnapshot riskSnapshot = batchRiskResolver.resolve(batch);
        boolean canResume = batchRiskResolver.canResume(batch);
        List<BatchActionVO> actions = buildActions(batch);
        BatchStatusFlowAdvisor.RecommendedAction recommendedAction = batchStatusFlowAdvisor.recommendAction(
                batch,
                latestTrace != null,
                latestQuality != null && !"FAIL".equalsIgnoreCase(latestQuality.result()),
                batch.getQrCode() != null,
                canResume
        );
        boolean publishReady = actions.stream()
                .anyMatch(action -> ("PUBLISH".equals(action.code()) || "RESUME".equals(action.code())) && action.enabled());
        boolean draftPending = assignedDraft != null;
        return new BatchListItemVO(
                batch.getId(),
                batch.getBatchCode(),
                batch.getProduct().name(),
                batch.getProduct().imageUrl(),
                batch.getCompany().name(),
                batch.getStatus().name(),
                toStatusLabel(batch.getStatus()),
                batch.getCurrentNode(),
                batch.getOriginPlace(),
                formatDate(batch.getProductionDate()),
                formatDateTime(batch.getPublishedAt()),
                qrStatus,
                TraceDisplayLabels.qrStatus(qrStatus),
                batch.getQrCode() == null ? null : batch.getQrCode().token(),
                qualityStatusCode,
                qualityStatusLabel,
                publishReady,
                riskSnapshot.status(),
                riskSnapshot.statusLabel(),
                latestRiskActionLabel(batch, riskSnapshot),
                riskResolutionLabel(riskSnapshot),
                canResume,
                actions,
                buildQuickTags(batch, latestQuality),
                formatDateTime(latestActivityAt(batch)),
                formatDateTime(latestTrace == null ? null : latestTrace.eventTime()),
                batch.getAssigneeUserId(),
                batch.getAssigneeName(),
                formatDateTime(batch.getAssignedAt()),
                effectiveTaskStatus,
                draftPending ? TraceDisplayLabels.draftStatus(true) : toTaskStatusLabel(effectiveTaskStatus),
                formatDateTime(effectiveTaskCompletedAt),
                isTaskCompletedToday(batch, currentUser),
                draftPending,
                TraceDisplayLabels.draftStatus(draftPending),
                formatDateTime(assignedDraft == null ? null : assignedDraft.getUpdatedAt()),
                recommendedAction.code(),
                recommendedAction.label(),
                recommendedAction.hint()
        );
    }

    private BatchWorkbenchVO toWorkbench(BatchEntity batch) {
        List<TraceRecordVO> sortedTraceRecords = batch.getTraceRecords().stream()
                .sorted(Comparator.comparing(TraceRecordEntity::eventTime).reversed())
                .map(this::toTraceRecordVO)
                .toList();
        List<QualityReportVO> sortedQualityReports = batch.getQualityReports().stream()
                .sorted(Comparator.comparing(QualityReportEntity::reportTime).reversed())
                .map(this::toQualityVO)
                .toList();
        List<BatchStatusLogVO> statusHistory = batch.getStatusHistory().stream()
                .sorted(Comparator.comparing(StatusHistoryEntity::operatedAt).reversed())
                .map(this::toStatusLogVO)
                .toList();
        QualityReportVO latestQuality = sortedQualityReports.isEmpty() ? null : sortedQualityReports.get(0);
        String effectiveTaskStatus = resolveEffectiveTaskStatus(batch);
        LocalDateTime effectiveTaskCompletedAt = resolveEffectiveTaskCompletedAt(batch);
        BatchFieldDraftPO draftPO = findFieldDraftPO(batch.getId(), batch.getAssigneeUserId());
        boolean draftPending = draftPO != null;

        return new BatchWorkbenchVO(
                new BatchOverviewVO(
                        batch.getId(),
                        batch.getBatchCode(),
                        batch.getOriginPlace(),
                        formatDate(batch.getProductionDate()),
                        formatDateTime(batch.getPublishedAt()),
                        batch.getPublicRemark(),
                        batch.getInternalRemark(),
                        batch.getProduct().imageUrl()
                ),
                new BatchTaskSummaryVO(
                        batch.getAssigneeUserId(),
                        batch.getAssigneeName(),
                        formatDateTime(batch.getAssignedAt()),
                        effectiveTaskStatus,
                        draftPending ? TraceDisplayLabels.draftStatus(true) : toTaskStatusLabel(effectiveTaskStatus),
                        formatDateTime(effectiveTaskCompletedAt),
                        isTaskCompletedToday(batch),
                        draftPending,
                        TraceDisplayLabels.draftStatus(draftPending),
                        formatDateTime(draftPO == null ? null : draftPO.getUpdatedAt())
                ),
                new ProductSummaryVO(
                        batch.getProduct().id(),
                        batch.getProduct().companyId(),
                        batch.getProduct().name(),
                        batch.getProduct().category(),
                        batch.getProduct().specification(),
                        batch.getProduct().unit(),
                        batch.getProduct().imageUrl()
                ),
                new CompanySummaryVO(
                        batch.getCompany().id(),
                        batch.getCompany().name(),
                        batch.getCompany().licenseNo(),
                        batch.getCompany().contactName(),
                        batch.getCompany().contactPhone(),
                        batch.getCompany().address()
                ),
                buildStatusSummary(batch, statusHistory),
                buildRiskSummary(batch),
                buildRiskHandlingSection(batch),
                new TraceSectionVO(
                        sortedTraceRecords.size(),
                        sortedTraceRecords.isEmpty() ? null : sortedTraceRecords.get(0).eventTime(),
                        "优先用快速录入补齐关键节点，只填阶段、地点、说明和时间。",
                        sortedTraceRecords.stream().limit(6).toList()
                ),
                new QualitySectionVO(
                        latestQuality == null ? "PENDING" : latestQuality.result(),
                        latestQuality == null ? TraceDisplayLabels.qualityStatus(null) : latestQuality.resultLabel(),
                        latestQuality == null ? "当前尚未上传质检摘要，发布前应先补齐检测结论。" : buildQualitySummary(latestQuality),
                        sortedQualityReports.size(),
                        latestQuality,
                        sortedQualityReports
                ),
                toQrSummary(batch.getQrCode()),
                buildScanStats(batch),
                statusHistory,
                buildActions(batch)
        );
    }

    private BatchStatusSummaryVO buildStatusSummary(BatchEntity batch, List<BatchStatusLogVO> statusHistory) {
        BatchStatusLogVO latestStatusLog = statusHistory.isEmpty() ? null : statusHistory.get(0);
        return new BatchStatusSummaryVO(
                batch.getStatus().name(),
                toStatusLabel(batch.getStatus()),
                batch.getCurrentNode(),
                defaultValue(batch.getStatusReason(), latestStatusLog == null ? "" : latestStatusLog.reason()),
                latestStatusLog == null ? null : latestStatusLog.operatorName(),
                latestStatusLog == null ? null : latestStatusLog.operatedAt()
        );
    }

    private BatchRiskSummaryVO buildRiskSummary(BatchEntity batch) {
        BatchRiskResolver.RiskSnapshot risk = batchRiskResolver.resolve(batch);
        return new BatchRiskSummaryVO(
                risk.hasRisk(),
                risk.status(),
                risk.statusLabel(),
                risk.riskLevel(),
                risk.title(),
                risk.reason(),
                formatDateTime(risk.updatedAt()),
                risk.tip()
        );
    }

    private RiskHandlingSectionVO buildRiskHandlingSection(BatchEntity batch) {
        List<BatchRiskActionVO> history = batch.getRiskActions().stream()
                .sorted(Comparator.comparing(BatchRiskActionEntity::createdAt).reversed())
                .map(this::toRiskActionVO)
                .toList();
        return new RiskHandlingSectionVO(
                batchRiskResolver.currentHandlingStage(batch),
                batchRiskResolver.currentHandlingStageLabel(batch),
                batchRiskResolver.canResume(batch),
                history
        );
    }

    private QrSummaryVO toQrSummary(QrCodeEntity qrCode) {
        if (qrCode == null) {
            return new QrSummaryVO(null, null, "NOT_GENERATED", TraceDisplayLabels.qrStatus("NOT_GENERATED"), null, null, null, null, 0, 0, false);
        }
        return new QrSummaryVO(
                qrCode.id(),
                qrCode.token(),
                qrCode.status(),
                TraceDisplayLabels.qrStatus(qrCode.status()),
                qrCode.publicUrl(),
                qrCode.imageUrl(),
                formatDateTime(qrCode.generatedAt()),
                formatDateTime(qrCode.lastScanAt()),
                qrCode.pv(),
                qrCode.uv(),
                true
        );
    }

    private ScanStatsSectionVO buildScanStats(BatchEntity batch) {
        List<ScanRecordEntity> sortedRecords = batch.getScanRecords().stream()
                .sorted(Comparator.comparing(ScanRecordEntity::scanTime).reversed())
                .toList();
        long pv = batch.getQrCode() == null ? 0 : batch.getQrCode().pv();
        long uv = batch.getQrCode() == null ? 0 : batch.getQrCode().uv();
        LocalDate today = LocalDate.now();
        List<ScanTrendPointVO> trend = new ArrayList<>();
        for (int offset = 6; offset >= 0; offset--) {
            LocalDate day = today.minusDays(offset);
            List<ScanRecordEntity> dayRecords = sortedRecords.stream()
                    .filter(record -> record.scanTime() != null && day.equals(record.scanTime().toLocalDate()))
                    .toList();
            long dayUv = dayRecords.stream()
                    .map(record -> visitorKey(record.ip(), record.userAgent()))
                    .filter(this::notBlank)
                    .distinct()
                    .count();
            trend.add(new ScanTrendPointVO(formatDate(day), dayRecords.size(), dayUv));
        }
        return new ScanStatsSectionVO(
                pv,
                uv,
                formatDateTime(sortedRecords.isEmpty() ? null : sortedRecords.get(0).scanTime()),
                sortedRecords.stream()
                        .limit(5)
                        .map(record -> new ScanRecordVO(
                                formatDateTime(record.scanTime()),
                                record.ip(),
                                summarizeDevice(record.userAgent()),
                                defaultValue(record.referer(), "")
                        ))
                        .toList(),
                trend
        );
    }

    private FileAssetVO toFileAssetVO(FileAssetEntity asset) {
        return new FileAssetVO(
                asset.id(),
                asset.fileName(),
                asset.filePath(),
                asset.fileUrl(),
                asset.contentType(),
                asset.size(),
                asset.businessType(),
                asset.businessId()
        );
    }

    private FileAssetVO toFileAssetVO(BizAttachmentPO attachmentPO) {
        return new FileAssetVO(
                attachmentPO.getId(),
                attachmentPO.getFileName(),
                attachmentPO.getFilePath(),
                attachmentPO.getFileUrl(),
                attachmentPO.getContentType(),
                defaultLong(attachmentPO.getSize()),
                attachmentPO.getBusinessType(),
                attachmentPO.getBusinessId()
        );
    }

    private FieldDraftVO toFieldDraftVO(BatchFieldDraftPO draftPO) {
        if (draftPO == null) {
            return null;
        }
        TraceBatchPO batchPO = traceBatchMapper.selectById(draftPO.getBatchId());
        return toFieldDraftVO(draftPO, batchPO);
    }

    private FieldDraftVO toFieldDraftVO(BatchFieldDraftPO draftPO, TraceBatchPO batchPO) {
        if (draftPO == null || batchPO == null) {
            return null;
        }
        BatchEntity batch = loadBatchEntity(batchPO);
        List<FileAssetVO> uploadedFiles = readDraftFiles(draftPO.getUploadedFilesJson());
        return new FieldDraftVO(
                draftPO.getId(),
                batchPO.getId(),
                batchPO.getBatchCode(),
                batch.getProduct().name(),
                batch.getCompany().name(),
                batch.getCurrentNode(),
                defaultValue(draftPO.getStage(), TraceStage.PRODUCE.name()),
                defaultValue(draftPO.getTitle(), ""),
                formatDateTime(draftPO.getEventTime()),
                defaultValue(draftPO.getOperatorName(), defaultOperatorName(currentUser())),
                defaultValue(draftPO.getLocation(), batchPO.getOriginPlace()),
                defaultValue(draftPO.getSummary(), ""),
                defaultValue(draftPO.getImageUrl(), ""),
                readDraftAttachmentIds(draftPO.getAttachmentIdsJson()),
                uploadedFiles,
                !Boolean.FALSE.equals(draftPO.getVisibleToConsumer()),
                uploadedFiles.size(),
                formatDateTime(draftPO.getUpdatedAt())
        );
    }

    private TraceRecordVO toTraceRecordVO(TraceRecordEntity record) {
        return new TraceRecordVO(
                record.id(),
                record.stage().name(),
                record.stage().label(),
                record.title(),
                formatDateTime(record.eventTime()),
                record.operatorName(),
                record.location(),
                record.visibleToConsumer(),
                record.summary(),
                record.imageUrl(),
                record.attachments().stream().map(this::toFileAssetVO).toList()
        );
    }

    private QualityReportVO toQualityVO(QualityReportEntity report) {
        return new QualityReportVO(
                report.id(),
                report.reportNo(),
                report.agency(),
                report.result(),
                toQualityLabel(report.result()),
                formatDateTime(report.reportTime()),
                report.highlights(),
                report.attachments().stream().map(this::toFileAssetVO).toList()
        );
    }

    private BatchStatusLogVO toStatusLogVO(StatusHistoryEntity history) {
        return new BatchStatusLogVO(
                history.status().name(),
                toStatusLabel(history.status()),
                history.reason(),
                history.operatorName(),
                formatDateTime(history.operatedAt())
        );
    }

    private BatchRiskActionVO toRiskActionVO(BatchRiskActionEntity action) {
        return new BatchRiskActionVO(
                action.id(),
                action.actionType().code(),
                action.actionType().label(),
                action.reason(),
                action.comment(),
                action.operatorName(),
                formatDateTime(action.createdAt())
        );
    }

    private List<BatchActionVO> buildActions(BatchEntity batch) {
        return buildRoleAwareActions(batch);
    }

    private List<BatchActionVO> buildRoleAwareActions(BatchEntity batch) {
        AuthUserSession currentUser = currentUser();
        QualityReportEntity latestQuality = latestQuality(batch);
        boolean hasQualifiedReport = latestQuality != null && !"FAIL".equalsIgnoreCase(latestQuality.result());
        boolean hasQr = batch.getQrCode() != null;
        boolean canManageBatch = canManageCompanyBatch(currentUser, batch);
        boolean canEdit = canEditBatch(currentUser, batch);
        boolean canTrace = canWriteTrace(currentUser, batch);
        boolean canQuality = canManageBatch;
        boolean canQr = canManageBatch;
        boolean canRisk = canManageBatch;
        boolean canPublish = canManageBatch
                && batch.getStatus() == BatchStatus.DRAFT
                && hasQualifiedReport
                && hasQr;
        boolean canResume = canManageBatch
                && batch.getStatus() == BatchStatus.FROZEN
                && hasQualifiedReport
                && hasQr
                && batchRiskResolver.canResume(batch);
        boolean canFreeze = canRisk && batch.getStatus() == BatchStatus.PUBLISHED;
        boolean canRecall = canRisk && (batch.getStatus() == BatchStatus.PUBLISHED || batch.getStatus() == BatchStatus.FROZEN);
        boolean canGenerateQr = canQr && !hasQr;
        String publishBlockedReason = canManageBatch
                ? defaultValue(resolvePublishBlockedReason(batch, batch.getStatus()), "已满足发布条件。")
                : "当前账号不能发布批次。";

        return List.of(
                new BatchActionVO("EDIT", "编辑批次", canEdit, canEdit ? "可维护批次基础资料。" : "当前账号只能查看该批次资料。", "neutral"),
                new BatchActionVO("ADD_TRACE", "新增追溯记录", canTrace && batch.getStatus() != BatchStatus.RECALLED, canTrace ? "使用快速录入补齐关键节点。" : "当前账号不能补录追溯。", "primary"),
                new BatchActionVO("UPLOAD_QUALITY", "上传质检", canQuality && batch.getStatus() != BatchStatus.RECALLED, canQuality ? "发布前优先补齐质检摘要。" : "当前账号不能上传质检。", "success"),
                new BatchActionVO("GENERATE_QR", hasQr ? "查看二维码" : "生成二维码", canGenerateQr, hasQr ? "二维码已存在，可继续核对公开页。" : (canQr ? "同一批次默认只生成一次二维码。" : "当前账号不能生成二维码。"), "primary"),
                new BatchActionVO("VIEW_PUBLIC", "公开页预览", hasQr, hasQr ? "可直接打开公开追溯页。" : "需先生成二维码。", "neutral"),
                new BatchActionVO("PUBLISH", "发布批次", canPublish, canPublish ? "已满足发布条件。" : publishBlockedReason, "success"),
                new BatchActionVO("RESUME", "恢复发布", canResume, canResume ? "整改已完成，满足恢复发布条件。" : publishBlockedReason, "success"),
                new BatchActionVO("FREEZE", "冻结批次", canFreeze, canRisk ? "发现异常时应快速冻结，并写明原因。" : "当前账号不能处理风险状态。", "warning"),
                new BatchActionVO("RECALL", "召回批次", canRecall, canRisk ? "召回后公开页首页会展示风险提示。" : "当前账号不能发起召回。", "danger")
        );
    }

    private void validateStatusTransition(BatchEntity batch, BatchStatus currentStatus, BatchStatus targetStatus) {
        if (!batchStatusFlowAdvisor.canTransition(currentStatus, targetStatus)) {
            throw new IllegalArgumentException(batchStatusFlowAdvisor.blockedTransitionHint(currentStatus, targetStatus));
        }
        if (targetStatus == BatchStatus.PUBLISHED) {
            String blockedReason = resolvePublishBlockedReason(batch, currentStatus);
            if (blockedReason != null) {
                throw new IllegalArgumentException(blockedReason);
            }
        }
    }

    private String resolvePublishBlockedReason(BatchEntity batch, BatchStatus currentStatus) {
        if (!batchStatusFlowAdvisor.canTransition(currentStatus, BatchStatus.PUBLISHED)) {
            return batchStatusFlowAdvisor.blockedTransitionHint(currentStatus, BatchStatus.PUBLISHED);
        }

        QualityReportEntity latestQuality = latestQuality(batch);
        if (latestQuality == null) {
            return "发布前请先上传质检摘要";
        }
        if ("FAIL".equalsIgnoreCase(latestQuality.result())) {
            return "检测结果不合格，不能发布";
        }
        if (batch.getQrCode() == null) {
            return "发布前请先生成二维码";
        }
        if (currentStatus == BatchStatus.FROZEN && !batchRiskResolver.canResume(batch)) {
            return "请先补充处理意见并标记整改完成，再恢复发布";
        }
        return null;
    }

    private QualityReportEntity latestQuality(BatchEntity batch) {
        return batch.getQualityReports().stream()
                .max(Comparator.comparing(QualityReportEntity::reportTime))
                .orElse(null);
    }

    private TraceRecordEntity latestTrace(BatchEntity batch) {
        return batch.getTraceRecords().stream()
                .filter(Objects::nonNull)
                .filter(record -> record.eventTime() != null)
                .max(Comparator.comparing(TraceRecordEntity::eventTime))
                .orElse(null);
    }

    private LocalDateTime latestActivityAt(BatchEntity batch) {
        LocalDateTime latest = null;
        latest = laterOf(latest, batch.getPublishedAt());
        latest = laterOf(latest, batch.getFrozenAt());
        latest = laterOf(latest, batch.getRecalledAt());
        for (TraceRecordEntity record : batch.getTraceRecords()) {
            latest = laterOf(latest, record == null ? null : record.eventTime());
        }
        for (QualityReportEntity report : batch.getQualityReports()) {
            latest = laterOf(latest, report == null ? null : report.reportTime());
        }
        for (StatusHistoryEntity history : batch.getStatusHistory()) {
            latest = laterOf(latest, history == null ? null : history.operatedAt());
        }
        for (BatchRiskActionEntity action : batch.getRiskActions()) {
            latest = laterOf(latest, action == null ? null : action.createdAt());
        }
        if (batch.getQrCode() != null) {
            latest = laterOf(latest, batch.getQrCode().generatedAt());
            latest = laterOf(latest, batch.getQrCode().lastScanAt());
        }
        return latest;
    }

    private LocalDateTime laterOf(LocalDateTime current, LocalDateTime candidate) {
        if (candidate == null) {
            return current;
        }
        if (current == null || candidate.isAfter(current)) {
            return candidate;
        }
        return current;
    }

    private List<String> buildQuickTags(BatchEntity batch, QualityReportEntity latestQuality) {
        List<String> tags = new ArrayList<>();
        tags.add(toStatusLabel(batch.getStatus()));
        tags.add(batch.getQrCode() == null ? "待生成二维码" : TraceDisplayLabels.qrStatus(batch.getQrCode().status()));
        tags.add(latestQuality == null ? "待上传质检" : toQualityLabel(latestQuality.result()));
        if (batch.getQrCode() != null && batch.getQrCode().pv() > 0) {
            tags.add("已扫码 " + batch.getQrCode().pv() + " 次");
        }
        if (batch.getStatus() == BatchStatus.FROZEN || batch.getStatus() == BatchStatus.RECALLED) {
            tags.add(batchRiskResolver.currentHandlingStageLabel(batch));
        }
        return tags;
    }

    private String buildQualitySummary(QualityReportVO latestQuality) {
        if (latestQuality.highlights() == null || latestQuality.highlights().isEmpty()) {
            return latestQuality.resultLabel() + "，暂未补充更多检测亮点。";
        }
        return latestQuality.resultLabel() + "，重点结果：" + String.join("、", latestQuality.highlights());
    }

    private String latestRiskActionLabel(BatchEntity batch, BatchRiskResolver.RiskSnapshot riskSnapshot) {
        BatchRiskActionEntity latestAction = batch.getRiskActions().stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparing(BatchRiskActionEntity::createdAt))
                .orElse(null);
        if (latestAction == null) {
            return riskSnapshot.hasRisk() ? "暂无处理动作" : "暂无风险动作";
        }
        return latestAction.actionType().label() + " · " + defaultValue(latestAction.operatorName(), "未知操作人");
    }

    private String riskResolutionLabel(BatchRiskResolver.RiskSnapshot riskSnapshot) {
        String status = defaultValue(riskSnapshot == null ? null : riskSnapshot.status(), "NORMAL")
                .toUpperCase(Locale.ROOT);
        return switch (status) {
            case "PROCESSING" -> "整改处理中";
            case "RECTIFIED" -> "已完成整改";
            case "FROZEN", "RISK_PENDING" -> "待完成整改";
            case "RECALLED" -> "召回处理中";
            default -> "无需整改";
        };
    }

    private String resolveCurrentNode(BatchEntity batch, List<TraceRecordEntity> traceRecords) {
        String riskNode = batchRiskResolver.resolveWorkbenchNode(batch);
        if (riskNode != null) {
            return riskNode;
        }
        if (!traceRecords.isEmpty()) {
            TraceRecordEntity latest = traceRecords.stream()
                    .max(Comparator.comparing(TraceRecordEntity::eventTime))
                    .orElse(traceRecords.get(0));
            return latest.stage().label();
        }
        if (batch.getStatus() == BatchStatus.PUBLISHED) {
            return "已发布，可对外扫码查看";
        }
        return "待补录追溯记录";
    }

    private BatchStatus readBatchStatus(String value) {
        if (value == null || value.isBlank()) {
            return BatchStatus.DRAFT;
        }
        if ("ACTIVE".equalsIgnoreCase(value)) {
            return BatchStatus.PUBLISHED;
        }
        return BatchStatus.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private String normalizeStatusForQuery(String value) {
        if ("ACTIVE".equalsIgnoreCase(value)) {
            return BatchStatus.PUBLISHED.name();
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String toStatusLabel(BatchStatus status) {
        return TraceDisplayLabels.batchStatus(status);
    }

    private String toQrStatus(BatchStatus status) {
        return switch (status) {
            case DRAFT -> "READY";
            case PUBLISHED -> "ACTIVE";
            case FROZEN -> "SUSPENDED";
            case RECALLED -> "RECALLED";
        };
    }

    private String toQualityLabel(String result) {
        return TraceDisplayLabels.qualityStatus(result);
    }

    private String toTaskStatusLabel(String taskStatus) {
        return TraceDisplayLabels.taskStatus(taskStatus);
    }

    private String resolveEffectiveTaskStatus(BatchEntity batch) {
        if (batch == null) {
            return "PENDING";
        }
        String rawStatus = defaultValue(batch.getTaskStatus(), "PENDING").toUpperCase(Locale.ROOT);
        if ("DRAFT".equals(rawStatus)) {
            return "DRAFT";
        }
        if (isTaskCompletedToday(batch)) {
            return "COMPLETED";
        }
        return "PENDING";
    }

    private LocalDateTime resolveEffectiveTaskCompletedAt(BatchEntity batch) {
        return isTaskCompletedToday(batch) ? batch.getTaskCompletedAt() : null;
    }

    private boolean isTaskCompletedToday(BatchEntity batch) {
        return batch != null
                && batch.getAssigneeUserId() != null
                && batch.getTaskCompletedAt() != null
                && batch.getTaskCompletedAt().toLocalDate().equals(LocalDate.now());
    }

    private boolean isTaskCompletedToday(BatchEntity batch, AuthUserSession currentUser) {
        if (!isTaskCompletedToday(batch)) {
            return false;
        }
        if (!isOperator(currentUser)) {
            return true;
        }
        return Objects.equals(batch.getAssigneeUserId(), currentUser.userId());
    }

    private String resolveAssigneeName(SysUserPO assigneePO) {
        if (assigneePO == null) {
            return "";
        }
        String displayName = defaultValue(assigneePO.getRealName(), defaultValue(assigneePO.getUsername(), ""));
        if (assigneePO.getStatus() != null && assigneePO.getStatus() != 1) {
            return displayName + "（已停用）";
        }
        return displayName;
    }

    private String normalizeDraftStage(String stage) {
        if (!notBlank(stage)) {
            return TraceStage.PRODUCE.name();
        }
        try {
            return TraceStage.valueOf(stage.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException ignored) {
            return TraceStage.PRODUCE.name();
        }
    }

    private String defaultOperatorName(AuthUserSession currentUser) {
        if (currentUser == null) {
            return "现场操作员";
        }
        return defaultValue(currentUser.realName(), defaultValue(currentUser.username(), "现场操作员"));
    }

    private boolean matchesExtraFilters(BatchEntity batch, BatchListQueryRequest request) {
        if (request == null) {
            return true;
        }
        return containsIgnoreCase(batch.getProduct().name(), request.getProductName())
                && containsIgnoreCase(batch.getCompany().name(), request.getCompanyName());
    }

    private boolean containsIgnoreCase(String actual, String expected) {
        if (!notBlank(expected)) {
            return true;
        }
        if (actual == null) {
            return false;
        }
        return actual.toLowerCase(Locale.ROOT).contains(expected.trim().toLowerCase(Locale.ROOT));
    }

    private BatchFieldDraftPO findFieldDraftPO(Long batchId, AuthUserSession currentUser) {
        return findFieldDraftPO(batchId, currentUser == null ? null : currentUser.userId());
    }

    private BatchFieldDraftPO findFieldDraftPO(Long batchId, Long operatorUserId) {
        if (batchId == null || operatorUserId == null) {
            return null;
        }
        return batchFieldDraftMapper.selectOne(new LambdaQueryWrapper<BatchFieldDraftPO>()
                .eq(BatchFieldDraftPO::getBatchId, batchId)
                .eq(BatchFieldDraftPO::getOperatorUserId, operatorUserId)
                .last("limit 1"));
    }

    private void deleteFieldDraftRecord(Long batchId, AuthUserSession currentUser) {
        deleteFieldDraftRecord(batchId, currentUser == null ? null : currentUser.userId());
    }

    private void deleteFieldDraftRecord(Long batchId, Long operatorUserId) {
        if (batchId == null || operatorUserId == null) {
            return;
        }
        batchFieldDraftMapper.delete(new LambdaQueryWrapper<BatchFieldDraftPO>()
                .eq(BatchFieldDraftPO::getBatchId, batchId)
                .eq(BatchFieldDraftPO::getOperatorUserId, operatorUserId));
    }

    private void markTaskDrafting(TraceBatchPO batchPO) {
        if (batchPO == null) {
            return;
        }
        if (batchPO.getAssignedAt() == null && batchPO.getAssigneeUserId() != null) {
            batchPO.setAssignedAt(LocalDateTime.now());
        }
        batchPO.setTaskStatus("DRAFT");
        batchPO.setTaskCompletedAt(null);
        traceBatchMapper.updateById(batchPO);
    }

    private void markTaskCompleted(TraceBatchPO batchPO, AuthUserSession currentUser) {
        if (batchPO == null || !isOperator(currentUser) || !Objects.equals(batchPO.getAssigneeUserId(), currentUser.userId())) {
            return;
        }
        if (batchPO.getAssignedAt() == null) {
            batchPO.setAssignedAt(LocalDateTime.now());
        }
        batchPO.setTaskStatus("COMPLETED");
        batchPO.setTaskCompletedAt(LocalDateTime.now());
        traceBatchMapper.updateById(batchPO);
    }

    private void markTaskPendingIfNeeded(TraceBatchPO batchPO) {
        if (batchPO == null) {
            return;
        }
        if (batchPO.getTaskCompletedAt() != null && batchPO.getTaskCompletedAt().toLocalDate().equals(LocalDate.now())) {
            batchPO.setTaskStatus("COMPLETED");
        } else {
            batchPO.setTaskStatus("PENDING");
            batchPO.setTaskCompletedAt(null);
        }
        traceBatchMapper.updateById(batchPO);
    }

    private List<Long> sanitizeAttachmentIds(List<Long> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return List.of();
        }
        return attachmentIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<String> sanitizeQualityHighlights(List<String> highlights) {
        if (highlights == null || highlights.isEmpty()) {
            return List.of();
        }
        return highlights.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .limit(20)
                .toList();
    }

    private List<FileAssetVO> sanitizeDraftFiles(List<FieldDraftFileItemDTO> uploadedFiles) {
        if (uploadedFiles == null || uploadedFiles.isEmpty()) {
            return List.of();
        }
        return uploadedFiles.stream()
                .filter(Objects::nonNull)
                .map(item -> new FileAssetVO(
                        item.id(),
                        trimToNull(item.fileName()),
                        trimToNull(item.filePath()),
                        trimToNull(item.fileUrl()),
                        trimToNull(item.contentType()),
                        item.size() == null ? 0L : item.size(),
                        trimToNull(item.businessType()),
                        item.businessId()
                ))
                .toList();
    }

    private List<Long> readDraftAttachmentIds(String json) {
        if (!notBlank(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException ignored) {
            return List.of();
        }
    }

    private List<FileAssetVO> readDraftFiles(String json) {
        if (!notBlank(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException ignored) {
            return List.of();
        }
    }

    private AuthUserSession currentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return null;
        }
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof AuthUserSession userSession ? userSession : null;
    }

    private void writeOperationLog(AuthUserSession currentUser, Long companyId, String actionType, String targetType, Long targetId, String targetName, String summary) {
        if (currentUser == null || !notBlank(actionType)) {
            return;
        }
        operationLogService.record(new OperationLogRecord(
                currentUser.userId(),
                resolveActorName(currentUser),
                currentUser.roleCode(),
                companyId,
                actionType,
                targetType,
                targetId,
                targetName,
                "SUCCESS",
                summary
        ));
    }

    private String resolveActorName(AuthUserSession currentUser) {
        return defaultValue(currentUser == null ? null : currentUser.realName(), currentUser == null ? "" : currentUser.username());
    }

    private String resolveAssignmentActionType(SysUserPO previousAssigneePO, SysUserPO nextAssigneePO) {
        if (previousAssigneePO == null && nextAssigneePO != null) {
            return "BATCH_ASSIGN";
        }
        if (previousAssigneePO != null && nextAssigneePO == null) {
            return "BATCH_UNASSIGN";
        }
        return "BATCH_REASSIGN";
    }

    private String buildAssignmentSummary(String batchCode, SysUserPO previousAssigneePO, SysUserPO nextAssigneePO, boolean clearedDraft) {
        if (previousAssigneePO == null && nextAssigneePO != null) {
            return "将批次 " + batchCode + " 分配给 " + resolveUserDisplayName(nextAssigneePO);
        }
        if (previousAssigneePO != null && nextAssigneePO == null) {
            return "清空批次 " + batchCode + " 的分配，原分配人：" + resolveUserDisplayName(previousAssigneePO);
        }
        String summary = "将批次 " + batchCode + " 从 " + resolveUserDisplayName(previousAssigneePO) + " 改派给 " + resolveUserDisplayName(nextAssigneePO);
        if (clearedDraft) {
            return summary + "，并清除了原分配人的未提交草稿";
        }
        return summary;
    }

    private String resolveUserDisplayName(SysUserPO userPO) {
        return defaultValue(userPO == null ? null : userPO.getRealName(), userPO == null ? "" : userPO.getUsername());
    }

    private String resolveRiskActionLogType(RiskActionType actionType) {
        if (actionType == null) {
            return "RISK_COMMENT";
        }
        return switch (actionType) {
            case RECTIFICATION -> "RISK_RECTIFICATION";
            case PROCESSING -> "RISK_PROCESSING";
            case RECTIFIED -> "RISK_RECTIFIED";
            default -> "RISK_COMMENT";
        };
    }

    private String buildRiskActionSummary(String batchCode, BatchRiskActionCreateRequest request) {
        String detail = defaultValue(trimToNull(request == null ? null : request.comment()), trimToNull(request == null ? null : request.reason()));
        String summary = "为批次 " + batchCode + " 执行“" + (request == null || request.actionType() == null ? "风险处理" : request.actionType().label()) + "”";
        if (notBlank(detail)) {
            return summary + "，说明：" + detail;
        }
        return summary;
    }

    private AuthUserSession requireCurrentUser() {
        AuthUserSession currentUser = currentUser();
        if (currentUser == null) {
            throw new UnauthorizedException("当前登录状态已失效，请重新登录后再试。");
        }
        return currentUser;
    }

    private void applyBatchReadScope(LambdaQueryWrapper<TraceBatchPO> wrapper, AuthUserSession currentUser, boolean mineOnly) {
        if (wrapper == null || currentUser == null || isPlatformAdmin(currentUser) || isRegulator(currentUser)) {
            return;
        }
        if (isEnterpriseAdmin(currentUser)) {
            wrapper.eq(TraceBatchPO::getCompanyId, currentUser.companyId());
            return;
        }
        if (isOperator(currentUser)) {
            wrapper.eq(TraceBatchPO::getAssigneeUserId, currentUser.userId());
            if (currentUser.companyId() != null) {
                wrapper.eq(TraceBatchPO::getCompanyId, currentUser.companyId());
            }
            return;
        }
        if (mineOnly && currentUser.companyId() != null) {
            wrapper.eq(TraceBatchPO::getCompanyId, currentUser.companyId());
        }
    }

    private void ensureBatchAccessible(TraceBatchPO batchPO) {
        AuthUserSession currentUser = currentUser();
        if (currentUser == null || isPlatformAdmin(currentUser) || isRegulator(currentUser)) {
            return;
        }
        if (isEnterpriseAdmin(currentUser)) {
            if (!Objects.equals(batchPO.getCompanyId(), currentUser.companyId())) {
                denyBatchAccess(currentUser, batchPO, "你只能查看本企业批次。");
            }
            return;
        }
        if (!Objects.equals(batchPO.getAssigneeUserId(), currentUser.userId())) {
            denyTraceAction(currentUser, batchPO, "当前批次未分配给你，不能查看或提交现场记录。");
            throw new UnauthorizedException("当前批次未分配给你，无法查看或提交。");
        }
    }

    private void ensureAssignmentManager(AuthUserSession currentUser) {
        if (isPlatformAdmin(currentUser) || isEnterpriseAdmin(currentUser)) {
            return;
        }
        denyBatchAssignment(currentUser, null, "你没有批次任务分配权限。");
        throw new UnauthorizedException("你没有批次任务分配权限。");
    }

    private void ensureAssignmentManagerForBatch(AuthUserSession currentUser, TraceBatchPO batchPO) {
        ensureAssignmentManager(currentUser);
        if (isEnterpriseAdmin(currentUser) && !Objects.equals(currentUser.companyId(), batchPO.getCompanyId())) {
            denyBatchAssignment(currentUser, batchPO, "你无权改派该批次，只能管理本企业批次的任务分配。");
            throw new UnauthorizedException("你无权改派该批次，只能管理本企业批次的任务分配。");
        }
    }

    private Long normalizeOperatorLookupCompanyId(AuthUserSession currentUser, Long requestedCompanyId) {
        if (isEnterpriseAdmin(currentUser)) {
            if (requestedCompanyId != null && !Objects.equals(requestedCompanyId, currentUser.companyId())) {
                denyBatchAssignment(currentUser, null, "你无权查看其他企业的操作员列表。");
                throw new UnauthorizedException("你无权查看其他企业的操作员列表。");
            }
            return currentUser.companyId();
        }
        return requestedCompanyId;
    }

    private SysUserPO findAssignableOperatorRequired(Long operatorUserId, TraceBatchPO batchPO, AuthUserSession currentUser) {
        SysUserPO operatorPO = sysUserMapper.selectById(operatorUserId);
        if (operatorPO == null || operatorPO.getStatus() == null || operatorPO.getStatus() != 1) {
            throw new IllegalArgumentException("所选操作员不存在或已停用。");
        }
        if (!"OPERATOR".equalsIgnoreCase(operatorPO.getRoleCode())) {
            throw new IllegalArgumentException("所选用户不是可分配的操作员。");
        }
        if (isEnterpriseAdmin(currentUser) && !Objects.equals(operatorPO.getCompanyId(), currentUser.companyId())) {
            denyBatchAssignment(currentUser, batchPO, "你只能分配本企业的操作员。");
            throw new UnauthorizedException("你只能分配本企业的操作员。");
        }
        if (!Objects.equals(operatorPO.getCompanyId(), batchPO.getCompanyId())) {
            throw new IllegalArgumentException("所选操作员不属于当前批次所属企业。");
        }
        return operatorPO;
    }

    private void ensureBatchEditor(AuthUserSession currentUser, Long targetCompanyId, TraceBatchPO batchPO, String actionLabel) {
        if (isPlatformAdmin(currentUser)) {
            return;
        }
        if (isEnterpriseAdmin(currentUser)) {
            Long effectiveCompanyId = targetCompanyId != null ? targetCompanyId : (batchPO == null ? null : batchPO.getCompanyId());
            if (effectiveCompanyId == null || !Objects.equals(effectiveCompanyId, currentUser.companyId())) {
                denyBatchEdit(currentUser, batchPO, "你只能" + actionLabel + "本企业批次。");
            }
            return;
        }
        denyBatchEdit(currentUser, batchPO, "当前账号没有" + actionLabel + "权限。");
    }

    private void ensureStatusManager(AuthUserSession currentUser, TraceBatchPO batchPO, BatchStatus targetStatus) {
        if (isPlatformAdmin(currentUser)) {
            return;
        }
        if (isEnterpriseAdmin(currentUser) && Objects.equals(currentUser.companyId(), batchPO.getCompanyId())) {
            return;
        }
        denyQrPublish(currentUser, batchPO, "当前账号不能执行“" + statusActionLabel(targetStatus) + "”操作。");
    }

    private void ensureTraceWriter(AuthUserSession currentUser, TraceBatchPO batchPO) {
        if (isPlatformAdmin(currentUser)) {
            return;
        }
        if (isEnterpriseAdmin(currentUser) && Objects.equals(currentUser.companyId(), batchPO.getCompanyId())) {
            return;
        }
        if (isOperator(currentUser) && Objects.equals(currentUser.userId(), batchPO.getAssigneeUserId())) {
            return;
        }
        denyTraceAction(currentUser, batchPO, "当前账号不能为该批次补录追溯。");
    }

    private void ensureQualityWriter(AuthUserSession currentUser, TraceBatchPO batchPO) {
        if (canManageCompanyBatch(currentUser, batchPO)) {
            return;
        }
        denyQualityUpload(currentUser, batchPO, "当前账号不能上传质检。");
    }

    private void ensureRiskWriter(AuthUserSession currentUser, TraceBatchPO batchPO) {
        if (canManageCompanyBatch(currentUser, batchPO)) {
            return;
        }
        denyRiskAction(currentUser, batchPO, "当前账号不能处理风险动作。");
    }

    private void ensureQrPublisher(AuthUserSession currentUser, TraceBatchPO batchPO, String actionLabel) {
        if (canManageCompanyBatch(currentUser, batchPO)) {
            return;
        }
        denyQrPublish(currentUser, batchPO, "当前账号不能执行“" + actionLabel + "”操作。");
    }

    private boolean canManageCompanyBatch(AuthUserSession currentUser, TraceBatchPO batchPO) {
        if (currentUser == null || batchPO == null) {
            return false;
        }
        if (isPlatformAdmin(currentUser)) {
            return true;
        }
        return isEnterpriseAdmin(currentUser) && Objects.equals(currentUser.companyId(), batchPO.getCompanyId());
    }

    private boolean canManageCompanyBatch(AuthUserSession currentUser, BatchEntity batch) {
        if (currentUser == null || batch == null || batch.getCompany() == null) {
            return false;
        }
        if (isPlatformAdmin(currentUser)) {
            return true;
        }
        return isEnterpriseAdmin(currentUser) && Objects.equals(currentUser.companyId(), batch.getCompany().id());
    }

    private boolean canEditBatch(AuthUserSession currentUser, BatchEntity batch) {
        return canManageCompanyBatch(currentUser, batch);
    }

    private boolean canWriteTrace(AuthUserSession currentUser, BatchEntity batch) {
        if (batch == null || currentUser == null) {
            return false;
        }
        if (canManageCompanyBatch(currentUser, batch)) {
            return true;
        }
        return isOperator(currentUser) && Objects.equals(currentUser.userId(), batch.getAssigneeUserId());
    }

    private void denyBatchAccess(AuthUserSession currentUser, TraceBatchPO batchPO, String message) {
        recordDeniedOperation(currentUser, "BATCH_ACCESS_DENIED", "BATCH", batchPO, message);
        throw new ForbiddenException(message);
    }

    private void denyBatchEdit(AuthUserSession currentUser, TraceBatchPO batchPO, String message) {
        recordDeniedOperation(currentUser, "BATCH_EDIT_DENIED", "BATCH", batchPO, message);
        throw new ForbiddenException(message);
    }

    private void denyBatchAssignment(AuthUserSession currentUser, TraceBatchPO batchPO, String message) {
        recordDeniedOperation(currentUser, "BATCH_ASSIGN_DENIED", "BATCH", batchPO, message);
        throw new ForbiddenException(message);
    }

    private void denyTraceAction(AuthUserSession currentUser, TraceBatchPO batchPO, String message) {
        recordDeniedOperation(currentUser, "TRACE_RECORD_DENIED", "BATCH", batchPO, message);
        throw new ForbiddenException(message);
    }

    private void denyQualityUpload(AuthUserSession currentUser, TraceBatchPO batchPO, String message) {
        recordDeniedOperation(currentUser, "QUALITY_UPLOAD_DENIED", "QUALITY", batchPO, message);
        throw new ForbiddenException(message);
    }

    private void denyRiskAction(AuthUserSession currentUser, TraceBatchPO batchPO, String message) {
        recordDeniedOperation(currentUser, "RISK_ACTION_DENIED", "BATCH", batchPO, message);
        throw new ForbiddenException(message);
    }

    private void denyQrPublish(AuthUserSession currentUser, TraceBatchPO batchPO, String message) {
        recordDeniedOperation(currentUser, "QR_PUBLISH_DENIED", "QR", batchPO, message);
        throw new ForbiddenException(message);
    }

    private void recordDeniedOperation(AuthUserSession currentUser, String actionType, String targetType, TraceBatchPO batchPO, String summary) {
        if (currentUser == null) {
            return;
        }
        operationLogService.record(new OperationLogRecord(
                currentUser.userId(),
                defaultValue(currentUser.realName(), defaultValue(currentUser.username(), "系统用户")),
                currentUser.roleCode(),
                currentUser.companyId(),
                actionType,
                targetType,
                batchPO == null ? null : batchPO.getId(),
                batchPO == null ? null : batchPO.getBatchCode(),
                "FAILED",
                summary
        ));
    }

    private boolean isOperator(AuthUserSession currentUser) {
        return currentUser != null && "OPERATOR".equalsIgnoreCase(currentUser.roleCode());
    }

    private boolean isRegulator(AuthUserSession currentUser) {
        return currentUser != null && "REGULATOR".equalsIgnoreCase(currentUser.roleCode());
    }

    private boolean isPlatformAdmin(AuthUserSession currentUser) {
        return currentUser != null && "PLATFORM_ADMIN".equalsIgnoreCase(currentUser.roleCode());
    }

    private boolean isEnterpriseAdmin(AuthUserSession currentUser) {
        return currentUser != null && "ENTERPRISE_ADMIN".equalsIgnoreCase(currentUser.roleCode());
    }

    private String statusActionLabel(BatchStatus targetStatus) {
        if (targetStatus == null) {
            return "状态变更";
        }
        return switch (targetStatus) {
            case PUBLISHED -> "发布批次";
            case FROZEN -> "冻结批次";
            case RECALLED -> "召回批次";
            case DRAFT -> "调整为草稿";
        };
    }

    private void ensureBatchCodeUnique(String batchCode, Long ignoredBatchId) {
        TraceBatchPO existing = traceBatchMapper.selectOne(new LambdaQueryWrapper<TraceBatchPO>()
                .eq(TraceBatchPO::getBatchCode, batchCode.trim())
                .last("limit 1"));
        if (existing != null && !Objects.equals(existing.getId(), ignoredBatchId)) {
            throw new IllegalArgumentException("批次号已存在");
        }
    }

    private BaseProductPO findOrCreateProduct(String productName, String category) {
        BaseProductPO existing = baseProductMapper.selectOne(new LambdaQueryWrapper<BaseProductPO>()
                .eq(BaseProductPO::getName, productName.trim())
                .eq(BaseProductPO::getCategory, category.trim())
                .orderByAsc(BaseProductPO::getId)
                .last("limit 1"));
        if (existing != null) {
            if (!notBlank(existing.getImageUrl())) {
                existing.setImageUrl(resolveProductImage(existing.getName(), existing.getCategory()));
                baseProductMapper.updateById(existing);
            }
            return existing;
        }
        BaseProductPO productPO = new BaseProductPO();
        productPO.setName(productName.trim());
        productPO.setCategory(category.trim());
        productPO.setSpec("待补充");
        productPO.setUnit("待补充");
        productPO.setImageUrl(resolveProductImage(productName, category));
        baseProductMapper.insert(productPO);
        return productPO;
    }

    private OrgCompanyPO findOrCreateCompany(String companyName, String originPlace) {
        OrgCompanyPO existing = orgCompanyMapper.selectOne(new LambdaQueryWrapper<OrgCompanyPO>()
                .eq(OrgCompanyPO::getName, companyName.trim())
                .orderByAsc(OrgCompanyPO::getId)
                .last("limit 1"));
        if (existing != null) {
            if (!notBlank(existing.getAddress())) {
                existing.setAddress(originPlace.trim());
                orgCompanyMapper.updateById(existing);
            }
            return existing;
        }
        OrgCompanyPO companyPO = new OrgCompanyPO();
        companyPO.setName(companyName.trim());
        companyPO.setLicenseNo("AUTO-" + Math.abs(companyName.hashCode()));
        companyPO.setContact("待补充");
        companyPO.setPhone("待补充");
        companyPO.setAddress(originPlace.trim());
        orgCompanyMapper.insert(companyPO);
        return companyPO;
    }

    private BaseProductPO findProductRequired(Long productId) {
        BaseProductPO productPO = baseProductMapper.selectById(productId);
        if (productPO == null) {
            throw new IllegalArgumentException("productId does not exist");
        }
        if (!MasterDataStatus.fromCode(productPO.getStatus()).selectable()) {
            throw new IllegalArgumentException("selected product is not available");
        }
        if (!notBlank(productPO.getImageUrl())) {
            productPO.setImageUrl(resolveProductImage(productPO.getName(), productPO.getCategory()));
            baseProductMapper.updateById(productPO);
        }
        return productPO;
    }

    private OrgCompanyPO findCompanyRequired(Long companyId) {
        OrgCompanyPO companyPO = orgCompanyMapper.selectById(companyId);
        if (companyPO == null) {
            throw new IllegalArgumentException("companyId does not exist");
        }
        if (!MasterDataStatus.fromCode(companyPO.getStatus()).selectable()) {
            throw new IllegalArgumentException("selected company is not available");
        }
        return companyPO;
    }

    private void validateProductCompany(BaseProductPO productPO, Long companyId) {
        if (productPO.getCompanyId() != null && !Objects.equals(productPO.getCompanyId(), companyId)) {
            throw new IllegalArgumentException("product does not belong to the selected company");
        }
    }

    private BizAttachmentPO findAttachmentRequired(Long fileId) {
        BizAttachmentPO attachmentPO = bizAttachmentMapper.selectById(fileId);
        if (attachmentPO == null) {
            throw new IllegalArgumentException("attachment does not exist");
        }
        return attachmentPO;
    }

    private void validateRiskActionRequest(BatchRiskActionCreateRequest request) {
        boolean hasReason = notBlank(request.reason());
        boolean hasComment = notBlank(request.comment());
        if (!hasReason && !hasComment) {
            throw new IllegalArgumentException("reason or comment must be provided for risk handling");
        }
        if (request.actionType() == RiskActionType.COMMENT && !hasComment) {
            throw new IllegalArgumentException("comment is required for a handling opinion");
        }
        if (request.actionType() == RiskActionType.RECTIFICATION && !hasComment) {
            throw new IllegalArgumentException("comment is required for a rectification record");
        }
        if ((request.actionType() == RiskActionType.PROCESSING || request.actionType() == RiskActionType.RECTIFIED) && !hasReason) {
            throw new IllegalArgumentException("reason is required when marking processing or rectification completed");
        }
    }

    private void validateRiskActionFlow(BatchEntity batch, BatchRiskActionCreateRequest request) {
        List<BatchRiskActionEntity> actionsAfterAbnormal = riskActionsAfterAbnormal(batch);
        RiskActionType latestType = actionsAfterAbnormal.stream()
                .max(Comparator.comparing(BatchRiskActionEntity::createdAt))
                .map(BatchRiskActionEntity::actionType)
                .orElse(null);

        if (request.actionType() == RiskActionType.PROCESSING && latestType == RiskActionType.PROCESSING) {
            throw new IllegalArgumentException("当前已标记为处理中，请先补整改记录或更新下一阶段动作");
        }
        if (request.actionType() == RiskActionType.PROCESSING && latestType == RiskActionType.RECTIFIED) {
            throw new IllegalArgumentException("当前已标记整改完成，请勿回退到处理中");
        }
        if (request.actionType() == RiskActionType.RECTIFIED) {
            boolean hasHandlingContext = actionsAfterAbnormal.stream().anyMatch(action ->
                    action.actionType() == RiskActionType.COMMENT
                            || action.actionType() == RiskActionType.RECTIFICATION
                            || action.actionType() == RiskActionType.PROCESSING
            );
            if (!hasHandlingContext) {
                throw new IllegalArgumentException("请先补处理说明或整改记录，再标记已整改");
            }
            if (latestType == RiskActionType.RECTIFIED) {
                throw new IllegalArgumentException("当前已经标记为已整改，无需重复提交");
            }
        }
    }

    private List<BatchRiskActionEntity> riskActionsAfterAbnormal(BatchEntity batch) {
        LocalDateTime abnormalAt = abnormalAt(batch);
        return batch.getRiskActions().stream()
                .filter(item -> abnormalAt == null || !item.createdAt().isBefore(abnormalAt))
                .toList();
    }

    private LocalDateTime abnormalAt(BatchEntity batch) {
        if (batch.getStatus() == BatchStatus.RECALLED) {
            return batch.getRecalledAt();
        }
        if (batch.getStatus() == BatchStatus.FROZEN) {
            return batch.getFrozenAt();
        }
        return null;
    }

    private List<BizAttachmentPO> claimAttachments(List<Long> attachmentIds, AttachmentBusinessType businessType, Long existingBusinessId) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return List.of();
        }
        Set<Long> distinctIds = new LinkedHashSet<>(attachmentIds);
        List<BizAttachmentPO> attachments = new ArrayList<>();
        for (Long attachmentId : distinctIds) {
            BizAttachmentPO attachmentPO = findAttachmentRequired(attachmentId);
            if (!businessType.code().equalsIgnoreCase(attachmentPO.getBusinessType())) {
                throw new IllegalArgumentException("attachment businessType mismatch");
            }
            if (attachmentPO.getBusinessId() != null && !Objects.equals(attachmentPO.getBusinessId(), existingBusinessId)) {
                throw new IllegalArgumentException("attachment already linked");
            }
            attachments.add(attachmentPO);
        }
        return attachments;
    }

    private void bindAttachmentsToBusiness(List<BizAttachmentPO> attachments, Long businessId) {
        for (BizAttachmentPO attachment : attachments) {
            attachment.setBusinessId(businessId);
            bizAttachmentMapper.updateById(attachment);
        }
    }

    private String firstAttachmentUrl(List<BizAttachmentPO> attachments, String fallbackUrl) {
        if (attachments != null && !attachments.isEmpty()) {
            return attachments.get(0).getFileUrl();
        }
        return trimToNull(fallbackUrl);
    }

    private void writeStatusLog(Long batchId, BatchStatus status, String reason, String operatorName, LocalDateTime operatedAt) {
        BatchStatusLogPO logPO = new BatchStatusLogPO();
        logPO.setBatchId(batchId);
        logPO.setStatus(status.name());
        logPO.setReason(reason);
        logPO.setOperatorName(operatorName);
        logPO.setOperatedAt(operatedAt);
        batchStatusLogMapper.insert(logPO);
    }

    private String resolveUniqueToken(String batchCode, Long batchId) {
        String baseToken = sanitizeToken(batchCode);
        String token = baseToken;
        QrCodePO existing = qrCodeMapper.selectOne(new LambdaQueryWrapper<QrCodePO>()
                .eq(QrCodePO::getQrToken, token)
                .last("limit 1"));
        if (existing != null && !Objects.equals(existing.getBatchId(), batchId)) {
            token = baseToken + "-" + batchId;
        }
        return token;
    }

    private String sanitizeToken(String batchCode) {
        String normalized = batchCode.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]+", "-");
        return normalized.replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
    }

    private String resolveProductImage(String productName, String category) {
        String lowered = (defaultValue(productName, "") + " " + defaultValue(category, "")).toLowerCase(Locale.ROOT);
        if (lowered.contains("橙") || lowered.contains("fruit") || lowered.contains("水果")) {
            return "/images/products/orange-batch.svg";
        }
        if (lowered.contains("茶")) {
            return "/images/products/green-tea-batch.svg";
        }
        return "/images/products/rice-batch.svg";
    }

    private String writeTraceContent(String summary, String imageUrl) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("summary", defaultValue(summary, "未填写摘要说明"));
        if (notBlank(imageUrl)) {
            payload.put("imageUrl", imageUrl.trim());
        }
        return writeJson(payload);
    }

    private String writeAttachments(String imageUrl) {
        if (!notBlank(imageUrl)) {
            return "[]";
        }
        return writeJson(List.of(imageUrl.trim()));
    }

    private String writeAttachments(List<BizAttachmentPO> attachments, String imageUrl) {
        List<FileAssetVO> payload = new ArrayList<>();
        if (attachments != null) {
            attachments.forEach(attachment -> payload.add(toFileAssetVO(attachment)));
        }
        if (payload.isEmpty() && notBlank(imageUrl)) {
            payload.add(new FileAssetVO(null, imageUrl.trim(), imageUrl.trim(), imageUrl.trim(), "image/*", 0, AttachmentBusinessType.TRACE_IMAGE.code(), null));
        }
        return writeJson(payload);
    }

    private String writeQualityJson(List<String> highlights) {
        return writeJson(Map.of("highlights", highlights == null ? List.of() : highlights));
    }

    private String writeQualityJson(List<String> highlights, List<BizAttachmentPO> attachments) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("highlights", highlights == null ? List.of() : highlights);
        payload.put("attachments", attachments == null ? List.of() : attachments.stream().map(this::toFileAssetVO).toList());
        return writeJson(payload);
    }

    private String readTraceSummary(String contentJson) {
        try {
            if (!notBlank(contentJson)) {
                return "未填写摘要说明";
            }
            JsonNode node = objectMapper.readTree(contentJson);
            if (node.hasNonNull("summary")) {
                return node.get("summary").asText();
            }
            if (node.has("fields") && node.get("fields").hasNonNull("remark")) {
                return node.get("fields").get("remark").asText();
            }
        } catch (JsonProcessingException ignored) {
        }
        return "未填写摘要说明";
    }

    private String readTraceImage(String attachmentsJson, String contentJson) {
        try {
            List<FileAssetEntity> attachments = readAttachmentAssets(attachmentsJson, null, AttachmentBusinessType.TRACE_IMAGE.code(), null);
            if (!attachments.isEmpty() && notBlank(attachments.get(0).fileUrl())) {
                return attachments.get(0).fileUrl();
            }
            if (notBlank(attachmentsJson)) {
                List<String> legacyAttachments = objectMapper.readValue(attachmentsJson, new TypeReference<>() {
                });
                if (!legacyAttachments.isEmpty() && notBlank(legacyAttachments.get(0))) {
                    return legacyAttachments.get(0);
                }
            }
            if (notBlank(contentJson)) {
                JsonNode node = objectMapper.readTree(contentJson);
                if (node.hasNonNull("imageUrl")) {
                    return node.get("imageUrl").asText();
                }
            }
        } catch (JsonProcessingException ignored) {
        }
        return null;
    }

    private List<String> readQualityHighlights(String reportJson) {
        try {
            if (!notBlank(reportJson)) {
                return List.of();
            }
            JsonNode node = objectMapper.readTree(reportJson);
            if (node.has("highlights") && node.get("highlights").isArray()) {
                List<String> highlights = new ArrayList<>();
                node.get("highlights").forEach(item -> highlights.add(item.asText()));
                return highlights;
            }
            if (node.isObject()) {
                List<String> values = new ArrayList<>();
                node.fields().forEachRemaining(entry -> values.add(entry.getValue().asText()));
                return values.stream().filter(this::notBlank).toList();
            }
        } catch (JsonProcessingException ignored) {
        }
        return List.of();
    }

    private List<FileAssetEntity> readQualityAttachments(QualityReportPO reportPO) {
        List<FileAssetEntity> attachments = readAttachmentAssets(
                reportPO.getReportJson(),
                "attachments",
                AttachmentBusinessType.QUALITY_ATTACHMENT.code(),
                reportPO.getId()
        );
        if (!attachments.isEmpty()) {
            return attachments;
        }
        if (notBlank(reportPO.getReportFileUrl())) {
            return List.of(new FileAssetEntity(
                    null,
                    reportPO.getReportFileUrl(),
                    reportPO.getReportFileUrl(),
                    reportPO.getReportFileUrl(),
                    "application/octet-stream",
                    0,
                    AttachmentBusinessType.QUALITY_ATTACHMENT.code(),
                    reportPO.getId()
            ));
        }
        return List.of();
    }

    private List<FileAssetEntity> readAttachmentAssets(String json, String fieldName, String businessType, Long businessId) {
        if (!notBlank(json)) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode attachmentNode = node;
            if (fieldName != null && node.isObject()) {
                attachmentNode = node.get(fieldName);
            }
            if (attachmentNode == null || attachmentNode.isNull()) {
                return List.of();
            }
            if (attachmentNode.isArray()) {
                List<FileAssetEntity> attachments = new ArrayList<>();
                for (JsonNode item : attachmentNode) {
                    FileAssetEntity asset = readAttachmentAsset(item, businessType, businessId);
                    if (asset != null) {
                        attachments.add(asset);
                    }
                }
                return attachments;
            }
            FileAssetEntity asset = readAttachmentAsset(attachmentNode, businessType, businessId);
            return asset == null ? List.of() : List.of(asset);
        } catch (JsonProcessingException ignored) {
            return List.of();
        }
    }

    private FileAssetEntity readAttachmentAsset(JsonNode node, String businessType, Long businessId) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            String url = node.asText();
            if (!notBlank(url)) {
                return null;
            }
            return new FileAssetEntity(null, url, url, url, "application/octet-stream", 0, businessType, businessId);
        }
        if (!node.isObject()) {
            return null;
        }
        Long id = node.hasNonNull("id") ? Long.valueOf(node.get("id").asLong()) : null;
        String fileName = node.hasNonNull("fileName") ? node.get("fileName").asText() : null;
        String filePath = node.hasNonNull("filePath") ? node.get("filePath").asText() : null;
        String fileUrl = node.hasNonNull("fileUrl") ? node.get("fileUrl").asText() : null;
        String contentType = node.hasNonNull("contentType") ? node.get("contentType").asText() : "application/octet-stream";
        long size = node.hasNonNull("size") ? node.get("size").asLong() : 0;
        String storedBusinessType = node.hasNonNull("businessType") ? node.get("businessType").asText() : businessType;
        Long storedBusinessId = node.hasNonNull("businessId") ? Long.valueOf(node.get("businessId").asLong()) : businessId;
        return new FileAssetEntity(id, fileName, filePath, fileUrl, contentType, size, storedBusinessType, storedBusinessId);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("JSON 序列化失败", exception);
        }
    }

    private String buildTraceHash(TraceEventPO eventPO, String prevHash) {
        String payload = String.join("|",
                "batchId=" + defaultLong(eventPO.getBatchId()),
                "stage=" + defaultValue(eventPO.getStage(), ""),
                "title=" + defaultValue(eventPO.getTitle(), ""),
                "eventTime=" + formatTraceHashTime(eventPO.getEventTime()),
                "operatorName=" + defaultValue(eventPO.getOperatorName(), ""),
                "location=" + defaultValue(eventPO.getLocation(), ""),
                "isPublic=" + (Boolean.TRUE.equals(eventPO.getIsPublic()) ? "1" : "0"),
                "contentJson=" + defaultValue(eventPO.getContentJson(), ""),
                "attachmentsJson=" + defaultValue(eventPO.getAttachmentsJson(), ""),
                "prevHash=" + defaultValue(prevHash, "")
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HEX_FORMAT.formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private String normalizeHash(String value) {
        return notBlank(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String formatTraceHashTime(LocalDateTime value) {
        return value == null ? "" : TRACE_HASH_TIME_FORMATTER.format(value);
    }

    private String visitorKey(QrQueryLogPO logPO) {
        return visitorKey(logPO.getIp(), logPO.getUa());
    }

    private String visitorKey(String ip, String userAgent) {
        return (defaultValue(ip, "unknown") + "|" + defaultValue(userAgent, "unknown")).trim();
    }

    private String summarizeDevice(String userAgent) {
        String normalized = defaultValue(userAgent, "").toLowerCase(Locale.ROOT);
        if (normalized.contains("mobile") || normalized.contains("iphone") || normalized.contains("android")) {
            return "Mobile";
        }
        if (normalized.contains("ipad") || normalized.contains("tablet")) {
            return "Tablet";
        }
        if (!normalized.isBlank()) {
            return "Desktop/Web";
        }
        return "Unknown";
    }

    private long defaultLong(Long value) {
        return value == null ? 0 : value;
    }

    private LocalDate parseRequiredDate(String value, String fieldName) {
        LocalDate date = parseNullableDate(value);
        if (date == null) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        return date;
    }

    private LocalDate parseNullableDate(String value) {
        if (!notBlank(value)) {
            return null;
        }
        String normalized = value.trim();
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(normalized, DATE_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(normalized, DATE_TIME_FORMATTER).toLocalDate();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(normalized).toLocalDate();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("日期格式错误，支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm");
        }
    }

    private LocalDateTime parseFlexibleDateTime(String value, LocalDateTime fallback) {
        if (!notBlank(value)) {
            return fallback;
        }
        String normalized = value.trim();
        try {
            return LocalDateTime.parse(normalized);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(normalized, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(normalized, DATE_FORMATTER).atTime(LocalTime.NOON);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("时间格式错误，支持 yyyy-MM-dd HH:mm 或 ISO-8601");
        }
    }

    private String formatDate(LocalDate value) {
        return value == null ? null : DATE_FORMATTER.format(value);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private String defaultValue(String value, String fallback) {
        return notBlank(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        return notBlank(value) ? value.trim() : null;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
