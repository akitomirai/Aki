package edu.jxust.agritrace.module.quality.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jxust.agritrace.common.util.HashUtil;
import edu.jxust.agritrace.module.quality.dto.QualityReportCreateRequest;
import edu.jxust.agritrace.module.quality.entity.HashNotary;
import edu.jxust.agritrace.module.quality.entity.QualityReport;
import edu.jxust.agritrace.module.quality.mapper.HashNotaryMapper;
import edu.jxust.agritrace.module.quality.mapper.QualityReportMapper;
import edu.jxust.agritrace.module.quality.service.QualityReportService;
import edu.jxust.agritrace.module.trace.entity.TraceEvent;
import edu.jxust.agritrace.module.trace.mapper.TraceEventMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 质检报告服务实现
 */
@Service
public class QualityReportServiceImpl implements QualityReportService {

    private final QualityReportMapper reportMapper;
    private final HashNotaryMapper notaryMapper;
    private final ObjectMapper objectMapper;
    private final TraceEventMapper traceEventMapper;
    private final ObjectMapper jsonMapper;

    public QualityReportServiceImpl(QualityReportMapper reportMapper,
                                    HashNotaryMapper notaryMapper,
                                    ObjectMapper objectMapper,
                                    TraceEventMapper traceEventMapper,
                                    ObjectMapper jsonMapper) {
        this.reportMapper = reportMapper;
        this.notaryMapper = notaryMapper;
        this.objectMapper = objectMapper;
        this.traceEventMapper = traceEventMapper;
        this.jsonMapper = jsonMapper;
    }

    private String normalize(JsonNode node) {
        try {
            return node == null ? "" : objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalArgumentException("reportJson 序列化失败", e);
        }
    }
    private String normalize(String json) {
        if (json == null || json.isBlank()) return "";
        try {
            JsonNode node = objectMapper.readTree(json);
            return objectMapper.writeValueAsString(node); // 统一成紧凑规范格式
        } catch (Exception e) {
            return json; // 理论上不该发生
        }
    }
    /**
     * 生成报告哈希的规则（毕业设计版）：
     * batchId + reportNo + agency + result + reportFileUrl + reportJson
     *
     * 说明：如果你未来要更严谨，可把“文件字节sha256”也拼进去（后面可升级）。
     */
    private String buildRaw(QualityReport r) {
        return safe(r.getBatchId())
                + "|" + safe(r.getReportNo())
                + "|" + safe(r.getAgency())
                + "|" + safe(r.getResult())
                + "|" + safe(r.getReportFileUrl())
                + "|" + normalize(r.getReportJson()); // 这里已经是规范 JSON 字符串
    }

    private String safe(Object o) { return o == null ? "" : String.valueOf(o); }

    @Override
    @Transactional
    public QualityReport createWithNotary(QualityReportCreateRequest req) {
        QualityReport r = new QualityReport();
        r.setBatchId(req.getBatchId());
        r.setReportNo(req.getReportNo());
        r.setAgency(req.getAgency());
        r.setResult(req.getResult());
        r.setReportFileUrl(req.getReportFileUrl());

        // ✅ 规整点：JsonNode -> 规范 JSON 字符串，再入库
        r.setReportJson(normalize(req.getReportJson()));

        reportMapper.insert(r);

        String sha = HashUtil.sha256Hex(buildRaw(r));
        HashNotary n = new HashNotary();
        n.setBizType("QUALITY_REPORT");
        n.setBizId(r.getId());
        n.setSha256(sha);
        n.setRemark("quality report notary");
        notaryMapper.insert(n);

        // 3) 自动补一条 INSPECT 事件，串到时间轴里
        TraceEvent e = new TraceEvent();

        e.setBatchId(r.getBatchId());
        e.setStage("INSPECT");
        e.setEventTime(java.time.LocalDateTime.now());
        e.setLocation(r.getAgency());

        try {
            String contentJson = objectMapper.writeValueAsString(java.util.Map.of(
                    "fields", java.util.Map.of(
                            "reportId", r.getId(),
                            "reportNo", r.getReportNo(),
                            "agency", r.getAgency(),
                            "result", r.getResult(),
                            "reportFileUrl", r.getReportFileUrl()
                    )
            ));
            e.setContentJson(contentJson);
        } catch (Exception ex) {
            throw new RuntimeException("生成检验事件JSON失败", ex);
        }

        e.setAttachmentsJson(null);
        e.setOperatorId(null);

        traceEventMapper.insert(e);

        return r;
    }

    @Override
    public boolean verifyLatest(long batchId) {
        QualityReport r = reportMapper.selectOne(new LambdaQueryWrapper<QualityReport>()
                .eq(QualityReport::getBatchId, batchId)
                .orderByDesc(QualityReport::getId)
                .last("limit 1"));
        if (r == null) return true;

        HashNotary n = notaryMapper.selectOne(new LambdaQueryWrapper<HashNotary>()
                .eq(HashNotary::getBizType, "QUALITY_REPORT")
                .eq(HashNotary::getBizId, r.getId())
                .last("limit 1"));
        if (n == null) return false;

        String sha = HashUtil.sha256Hex(buildRaw(r));

        return sha.equalsIgnoreCase(n.getSha256());
    }

    @Override
    @Transactional
    public boolean reNotary(long reportId) {
        QualityReport r = reportMapper.selectById(reportId);
        if (r == null) return false;

        String sha = HashUtil.sha256Hex(buildRaw(r)); // buildRaw 内已 normalize

        HashNotary n = notaryMapper.selectOne(new LambdaQueryWrapper<HashNotary>()
                .eq(HashNotary::getBizType, "QUALITY_REPORT")
                .eq(HashNotary::getBizId, reportId)
                .last("limit 1"));

        if (n == null) {
            HashNotary nn = new HashNotary();
            nn.setBizType("QUALITY_REPORT");
            nn.setBizId(reportId);
            nn.setSha256(sha);
            nn.setRemark("quality report re-notary");
            notaryMapper.insert(nn);
            return true;
        }

        n.setSha256(sha);
        notaryMapper.updateById(n);
        return true;
    }
}