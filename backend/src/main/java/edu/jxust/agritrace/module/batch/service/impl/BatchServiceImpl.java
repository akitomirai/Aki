package edu.jxust.agritrace.module.batch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jxust.agritrace.module.batch.dto.BatchCreateRequest;
import edu.jxust.agritrace.module.batch.dto.BatchListQueryRequest;
import edu.jxust.agritrace.module.batch.dto.BatchStatusActionRequest;
import edu.jxust.agritrace.module.batch.dto.BatchUpdateRequest;
import edu.jxust.agritrace.module.batch.dto.QualityReportCreateRequest;
import edu.jxust.agritrace.module.batch.dto.TraceRecordCreateRequest;
import edu.jxust.agritrace.module.batch.entity.BatchEntity;
import edu.jxust.agritrace.module.batch.entity.BatchStatus;
import edu.jxust.agritrace.module.batch.entity.CompanyEntity;
import edu.jxust.agritrace.module.batch.entity.ProductEntity;
import edu.jxust.agritrace.module.batch.entity.QrCodeEntity;
import edu.jxust.agritrace.module.batch.entity.QualityReportEntity;
import edu.jxust.agritrace.module.batch.entity.StatusHistoryEntity;
import edu.jxust.agritrace.module.batch.entity.TraceRecordEntity;
import edu.jxust.agritrace.module.batch.entity.TraceStage;
import edu.jxust.agritrace.module.batch.mapper.BaseProductMapper;
import edu.jxust.agritrace.module.batch.mapper.BatchStatusLogMapper;
import edu.jxust.agritrace.module.batch.mapper.OrgCompanyMapper;
import edu.jxust.agritrace.module.batch.mapper.QrCodeMapper;
import edu.jxust.agritrace.module.batch.mapper.QrQueryLogMapper;
import edu.jxust.agritrace.module.batch.mapper.QualityReportMapper;
import edu.jxust.agritrace.module.batch.mapper.TraceBatchMapper;
import edu.jxust.agritrace.module.batch.mapper.TraceEventMapper;
import edu.jxust.agritrace.module.batch.mapper.po.BaseProductPO;
import edu.jxust.agritrace.module.batch.mapper.po.BatchStatusLogPO;
import edu.jxust.agritrace.module.batch.mapper.po.OrgCompanyPO;
import edu.jxust.agritrace.module.batch.mapper.po.QrCodePO;
import edu.jxust.agritrace.module.batch.mapper.po.QrQueryLogPO;
import edu.jxust.agritrace.module.batch.mapper.po.QualityReportPO;
import edu.jxust.agritrace.module.batch.mapper.po.TraceBatchPO;
import edu.jxust.agritrace.module.batch.mapper.po.TraceEventPO;
import edu.jxust.agritrace.module.batch.service.BatchService;
import edu.jxust.agritrace.module.batch.service.support.QrImageStorageService;
import edu.jxust.agritrace.module.batch.service.support.TraceLinkBuilder;
import edu.jxust.agritrace.module.batch.vo.BatchActionVO;
import edu.jxust.agritrace.module.batch.vo.BatchListItemVO;
import edu.jxust.agritrace.module.batch.vo.BatchOverviewVO;
import edu.jxust.agritrace.module.batch.vo.BatchStatusLogVO;
import edu.jxust.agritrace.module.batch.vo.BatchStatusSummaryVO;
import edu.jxust.agritrace.module.batch.vo.BatchWorkbenchVO;
import edu.jxust.agritrace.module.batch.vo.CompanySummaryVO;
import edu.jxust.agritrace.module.batch.vo.ProductSummaryVO;
import edu.jxust.agritrace.module.batch.vo.QrSummaryVO;
import edu.jxust.agritrace.module.batch.vo.QualityReportVO;
import edu.jxust.agritrace.module.batch.vo.QualitySectionVO;
import edu.jxust.agritrace.module.batch.vo.TraceRecordVO;
import edu.jxust.agritrace.module.batch.vo.TraceSectionVO;
import edu.jxust.agritrace.module.publictrace.dto.PublicTraceAccessContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class BatchServiceImpl implements BatchService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final TraceBatchMapper traceBatchMapper;
    private final BaseProductMapper baseProductMapper;
    private final OrgCompanyMapper orgCompanyMapper;
    private final TraceEventMapper traceEventMapper;
    private final QualityReportMapper qualityReportMapper;
    private final QrCodeMapper qrCodeMapper;
    private final BatchStatusLogMapper batchStatusLogMapper;
    private final QrQueryLogMapper qrQueryLogMapper;
    private final ObjectMapper objectMapper;
    private final TraceLinkBuilder traceLinkBuilder;
    private final QrImageStorageService qrImageStorageService;

    public BatchServiceImpl(
            TraceBatchMapper traceBatchMapper,
            BaseProductMapper baseProductMapper,
            OrgCompanyMapper orgCompanyMapper,
            TraceEventMapper traceEventMapper,
            QualityReportMapper qualityReportMapper,
            QrCodeMapper qrCodeMapper,
            BatchStatusLogMapper batchStatusLogMapper,
            QrQueryLogMapper qrQueryLogMapper,
            ObjectMapper objectMapper,
            TraceLinkBuilder traceLinkBuilder,
            QrImageStorageService qrImageStorageService
    ) {
        this.traceBatchMapper = traceBatchMapper;
        this.baseProductMapper = baseProductMapper;
        this.orgCompanyMapper = orgCompanyMapper;
        this.traceEventMapper = traceEventMapper;
        this.qualityReportMapper = qualityReportMapper;
        this.qrCodeMapper = qrCodeMapper;
        this.batchStatusLogMapper = batchStatusLogMapper;
        this.qrQueryLogMapper = qrQueryLogMapper;
        this.objectMapper = objectMapper;
        this.traceLinkBuilder = traceLinkBuilder;
        this.qrImageStorageService = qrImageStorageService;
    }

    @Override
    public List<BatchListItemVO> listBatches(BatchListQueryRequest request) {
        LambdaQueryWrapper<TraceBatchPO> wrapper = new LambdaQueryWrapper<TraceBatchPO>()
                .orderByDesc(TraceBatchPO::getId);
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
    @Transactional(rollbackFor = Exception.class)
    public BatchWorkbenchVO createBatch(BatchCreateRequest request) {
        ensureBatchCodeUnique(request.batchCode(), null);
        BaseProductPO product = findOrCreateProduct(request.productName(), request.category());
        OrgCompanyPO company = findOrCreateCompany(request.companyName(), request.originPlace());

        TraceBatchPO batchPO = new TraceBatchPO();
        batchPO.setBatchCode(request.batchCode().trim());
        batchPO.setProductId(product.getId());
        batchPO.setCompanyId(company.getId());
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
        BaseProductPO product = findOrCreateProduct(request.productName(), request.category());
        OrgCompanyPO company = findOrCreateCompany(request.companyName(), request.originPlace());

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

        return toWorkbench(getBatchEntityById(batchId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchWorkbenchVO addTraceRecord(Long batchId, TraceRecordCreateRequest request) {
        TraceBatchPO batchPO = findBatchPO(batchId);
        LocalDateTime eventTime = parseFlexibleDateTime(request.eventTime(), LocalDateTime.now());
        TraceStage stage = request.stage() == null ? TraceStage.PRODUCE : request.stage();

        TraceEventPO eventPO = new TraceEventPO();
        eventPO.setBatchId(batchId);
        eventPO.setCompanyId(batchPO.getCompanyId());
        eventPO.setStage(stage.name());
        eventPO.setTitle(notBlank(request.title()) ? request.title().trim() : stage.label() + "补录");
        eventPO.setEventTime(eventTime);
        eventPO.setOperatorName(request.operatorName().trim());
        eventPO.setLocation(request.location().trim());
        eventPO.setIsPublic(request.visibleToConsumer());
        eventPO.setContentJson(writeTraceContent(request.summary(), request.imageUrl()));
        eventPO.setAttachmentsJson(writeAttachments(request.imageUrl()));
        traceEventMapper.insert(eventPO);

        return toWorkbench(getBatchEntityById(batchId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchWorkbenchVO addQualityReport(Long batchId, QualityReportCreateRequest request) {
        findBatchPO(batchId);
        LocalDateTime reportTime = parseFlexibleDateTime(request.reportTime(), LocalDateTime.now());

        QualityReportPO reportPO = new QualityReportPO();
        reportPO.setBatchId(batchId);
        reportPO.setReportNo(request.reportNo().trim());
        reportPO.setAgency(request.agency().trim());
        reportPO.setResult(request.result().trim().toUpperCase(Locale.ROOT));
        reportPO.setReportJson(writeQualityJson(request.highlights()));
        reportPO.setCreatedAt(reportTime);
        qualityReportMapper.insert(reportPO);

        return toWorkbench(getBatchEntityById(batchId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchWorkbenchVO generateQr(Long batchId) {
        TraceBatchPO batchPO = findBatchPO(batchId);
        QrCodePO existing = findQrByBatchId(batchId);
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
        } else {
            qrImageStorageService.ensureQrImage(existing.getQrToken(), traceLinkBuilder.buildPublicTraceUrl(existing.getQrToken()));
        }
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

    private TraceBatchPO findBatchPO(Long batchId) {
        TraceBatchPO batchPO = traceBatchMapper.selectById(batchId);
        if (batchPO == null) {
            throw new IllegalArgumentException("未找到对应批次");
        }
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

    private BatchEntity loadBatchEntity(TraceBatchPO batchPO) {
        BaseProductPO productPO = baseProductMapper.selectById(batchPO.getProductId());
        OrgCompanyPO companyPO = orgCompanyMapper.selectById(batchPO.getCompanyId());
        if (productPO == null || companyPO == null) {
            throw new IllegalArgumentException("批次关联的产品或企业信息缺失");
        }

        List<TraceRecordEntity> traceRecords = traceEventMapper.selectList(new LambdaQueryWrapper<TraceEventPO>()
                        .eq(TraceEventPO::getBatchId, batchPO.getId())
                        .orderByAsc(TraceEventPO::getEventTime)
                        .orderByAsc(TraceEventPO::getId))
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
        batch.setCurrentNode(resolveCurrentNode(batch.getStatus(), traceRecords));
        batch.setQrCode(toQrCodeEntity(qrCodePO, qrLogs));
        return batch;
    }

    private ProductEntity toProductEntity(BaseProductPO productPO) {
        return new ProductEntity(
                productPO.getId(),
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
        return new TraceRecordEntity(
                eventPO.getId(),
                TraceStage.fromCode(eventPO.getStage()),
                defaultValue(eventPO.getTitle(), TraceStage.fromCode(eventPO.getStage()).label()),
                eventPO.getEventTime(),
                defaultValue(eventPO.getOperatorName(), "未知操作人"),
                defaultValue(eventPO.getLocation(), "未填写地点"),
                Boolean.TRUE.equals(eventPO.getIsPublic()),
                readTraceSummary(eventPO.getContentJson()),
                readTraceImage(eventPO.getAttachmentsJson(), eventPO.getContentJson())
        );
    }

    private QualityReportEntity toQualityReportEntity(QualityReportPO reportPO) {
        return new QualityReportEntity(
                reportPO.getId(),
                reportPO.getReportNo(),
                defaultValue(reportPO.getAgency(), "未填写检测机构"),
                defaultValue(reportPO.getResult(), "REVIEW"),
                reportPO.getCreatedAt(),
                readQualityHighlights(reportPO.getReportJson())
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
                batch.getQrCode() == null ? "NOT_GENERATED" : batch.getQrCode().status(),
                latestQuality == null ? "待上传" : toQualityLabel(latestQuality.result()),
                buildActions(batch),
                buildQuickTags(batch, latestQuality)
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
                new ProductSummaryVO(
                        batch.getProduct().id(),
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
                new TraceSectionVO(
                        sortedTraceRecords.size(),
                        sortedTraceRecords.isEmpty() ? null : sortedTraceRecords.get(0).eventTime(),
                        "优先用快速录入补齐关键节点，只填阶段、地点、说明和时间。",
                        sortedTraceRecords.stream().limit(6).toList()
                ),
                new QualitySectionVO(
                        latestQuality == null ? "PENDING" : latestQuality.result(),
                        latestQuality == null ? "待上传" : latestQuality.resultLabel(),
                        latestQuality == null ? "当前尚未上传质检摘要，发布前应先补齐检测结论。" : buildQualitySummary(latestQuality),
                        sortedQualityReports.size(),
                        latestQuality,
                        sortedQualityReports
                ),
                toQrSummary(batch.getQrCode()),
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

    private QrSummaryVO toQrSummary(QrCodeEntity qrCode) {
        if (qrCode == null) {
            return new QrSummaryVO(null, null, "NOT_GENERATED", null, null, null, null, 0, 0, false);
        }
        return new QrSummaryVO(
                qrCode.id(),
                qrCode.token(),
                qrCode.status(),
                qrCode.publicUrl(),
                qrCode.imageUrl(),
                formatDateTime(qrCode.generatedAt()),
                formatDateTime(qrCode.lastScanAt()),
                qrCode.pv(),
                qrCode.uv(),
                true
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
                record.imageUrl()
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
                report.highlights()
        );
    }

    private BatchStatusLogVO toStatusLogVO(StatusHistoryEntity history) {
        return new BatchStatusLogVO(
                history.status().name(),
                history.reason(),
                history.operatorName(),
                formatDateTime(history.operatedAt())
        );
    }

    private List<BatchActionVO> buildActions(BatchEntity batch) {
        QualityReportEntity latestQuality = latestQuality(batch);
        boolean hasQualifiedReport = latestQuality != null && !"FAIL".equalsIgnoreCase(latestQuality.result());
        boolean hasQr = batch.getQrCode() != null;
        boolean canPublish = batch.getStatus() == BatchStatus.DRAFT && hasQualifiedReport && hasQr;
        boolean canResume = batch.getStatus() == BatchStatus.FROZEN && hasQualifiedReport && hasQr;
        boolean canFreeze = batch.getStatus() == BatchStatus.PUBLISHED;
        boolean canRecall = batch.getStatus() == BatchStatus.PUBLISHED || batch.getStatus() == BatchStatus.FROZEN;
        boolean canGenerateQr = !hasQr;

        return List.of(
                new BatchActionVO("EDIT", "编辑批次", true, "仅保留高频字段，避免长表单。", "neutral"),
                new BatchActionVO("ADD_TRACE", "新增追溯记录", batch.getStatus() != BatchStatus.RECALLED, "使用快速录入补齐关键节点。", "primary"),
                new BatchActionVO("UPLOAD_QUALITY", "上传质检", batch.getStatus() != BatchStatus.RECALLED, "发布前优先补齐高价值质检摘要。", "success"),
                new BatchActionVO("GENERATE_QR", hasQr ? "查看二维码" : "生成二维码", canGenerateQr, hasQr ? "已存在二维码，继续返回已有结果。" : "同一批次默认只生成一次二维码。", "primary"),
                new BatchActionVO("VIEW_PUBLIC", "公开页预览", hasQr, hasQr ? "可直接打开公开追溯页。" : "需先生成二维码。", "neutral"),
                new BatchActionVO("PUBLISH", "发布批次", canPublish, hasQualifiedReport && hasQr ? "已满足发布条件。" : "需先补齐合格质检和二维码。", "success"),
                new BatchActionVO("RESUME", "恢复发布", canResume, canResume ? "冻结问题已处理时可恢复公开展示。" : "仅冻结批次支持恢复。", "success"),
                new BatchActionVO("FREEZE", "冻结批次", canFreeze, "发现异常时应快速冻结，并写明原因。", "warning"),
                new BatchActionVO("RECALL", "召回批次", canRecall, "召回后公开页首屏需展示风险提示。", "danger")
        );
    }

    private void validateStatusTransition(BatchEntity batch, BatchStatus currentStatus, BatchStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return;
        }
        if (currentStatus == BatchStatus.RECALLED) {
            throw new IllegalArgumentException("已召回批次不能恢复到其他状态");
        }
        if (targetStatus == BatchStatus.PUBLISHED) {
            if (batch.getQrCode() == null) {
                throw new IllegalArgumentException("发布前请先生成二维码");
            }
            QualityReportEntity latestQuality = latestQuality(batch);
            if (latestQuality == null) {
                throw new IllegalArgumentException("发布前请先上传质检摘要");
            }
            if ("FAIL".equalsIgnoreCase(latestQuality.result())) {
                throw new IllegalArgumentException("检测结果不合格，不能发布");
            }
            if (currentStatus != BatchStatus.DRAFT && currentStatus != BatchStatus.FROZEN) {
                throw new IllegalArgumentException("当前状态不支持发布或恢复发布");
            }
        }
        if (targetStatus == BatchStatus.FROZEN && currentStatus != BatchStatus.PUBLISHED) {
            throw new IllegalArgumentException("只有已发布批次可以冻结");
        }
        if (targetStatus == BatchStatus.RECALLED && currentStatus != BatchStatus.PUBLISHED && currentStatus != BatchStatus.FROZEN) {
            throw new IllegalArgumentException("只有已发布或已冻结批次可以召回");
        }
    }

    private QualityReportEntity latestQuality(BatchEntity batch) {
        return batch.getQualityReports().stream()
                .max(Comparator.comparing(QualityReportEntity::reportTime))
                .orElse(null);
    }

    private List<String> buildQuickTags(BatchEntity batch, QualityReportEntity latestQuality) {
        List<String> tags = new ArrayList<>();
        tags.add(toStatusLabel(batch.getStatus()));
        tags.add(batch.getQrCode() == null ? "待生成二维码" : "支持扫码");
        tags.add(latestQuality == null ? "待上传质检" : toQualityLabel(latestQuality.result()));
        if (batch.getQrCode() != null && batch.getQrCode().pv() > 0) {
            tags.add("已扫码 " + batch.getQrCode().pv() + " 次");
        }
        if (batch.getStatus() == BatchStatus.FROZEN || batch.getStatus() == BatchStatus.RECALLED) {
            tags.add("风险批次");
        }
        return tags;
    }

    private String buildQualitySummary(QualityReportVO latestQuality) {
        if (latestQuality.highlights() == null || latestQuality.highlights().isEmpty()) {
            return latestQuality.resultLabel() + "，暂未补充更多检测亮点。";
        }
        return latestQuality.resultLabel() + "，重点结果：" + String.join("、", latestQuality.highlights());
    }

    private String resolveCurrentNode(BatchStatus status, List<TraceRecordEntity> traceRecords) {
        if (status == BatchStatus.RECALLED) {
            return "已召回，公开页需要明确提示风险";
        }
        if (status == BatchStatus.FROZEN) {
            return "已冻结，等待补充说明或恢复";
        }
        if (!traceRecords.isEmpty()) {
            TraceRecordEntity latest = traceRecords.stream()
                    .max(Comparator.comparing(TraceRecordEntity::eventTime))
                    .orElse(traceRecords.get(0));
            return latest.stage().label();
        }
        if (status == BatchStatus.PUBLISHED) {
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
        return switch (status) {
            case DRAFT -> "草稿";
            case PUBLISHED -> "已发布";
            case FROZEN -> "已冻结";
            case RECALLED -> "已召回";
        };
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
        if (Objects.equals(result, "PASS")) {
            return "合格";
        }
        if (Objects.equals(result, "FAIL")) {
            return "不合格";
        }
        return "待确认";
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

    private String writeQualityJson(List<String> highlights) {
        return writeJson(Map.of("highlights", highlights == null ? List.of() : highlights));
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
            if (notBlank(attachmentsJson)) {
                List<String> attachments = objectMapper.readValue(attachmentsJson, new TypeReference<>() {
                });
                if (!attachments.isEmpty() && notBlank(attachments.get(0))) {
                    return attachments.get(0);
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

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("JSON 序列化失败", exception);
        }
    }

    private String visitorKey(QrQueryLogPO logPO) {
        return (defaultValue(logPO.getIp(), "unknown") + "|" + defaultValue(logPO.getUa(), "unknown")).trim();
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
