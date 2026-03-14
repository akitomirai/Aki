package edu.jxust.agritrace.module.trace.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jxust.agritrace.module.batch.service.BatchParticipantService;
import edu.jxust.agritrace.module.qr.service.QrService;
import edu.jxust.agritrace.module.qr.vo.PublicBatchSimpleVO;
import edu.jxust.agritrace.module.qr.vo.PublicQrScanVO;
import edu.jxust.agritrace.module.quality.service.QualityReportService;
import edu.jxust.agritrace.module.regulation.service.RegulationService;
import edu.jxust.agritrace.module.trace.service.TraceEventService;
import edu.jxust.agritrace.module.trace.service.TracePublicService;
import edu.jxust.agritrace.module.trace.vo.PublicTraceDetailVO;
import edu.jxust.agritrace.module.trace.vo.PublicTraceTimelineVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 前台公开溯源服务实现
 */
@Slf4j
@Service
public class TracePublicServiceImpl implements TracePublicService {

    private final QrService qrService;
    private final TraceEventService traceEventService;
    private final QualityReportService qualityReportService;
    private final BatchParticipantService batchParticipantService;
    private final RegulationService regulationService;
    private final ObjectMapper objectMapper;

    public TracePublicServiceImpl(QrService qrService,
                                  TraceEventService traceEventService,
                                  QualityReportService qualityReportService,
                                  BatchParticipantService batchParticipantService,
                                  RegulationService regulationService,
                                  ObjectMapper objectMapper) {
        this.qrService = qrService;
        this.traceEventService = traceEventService;
        this.qualityReportService = qualityReportService;
        this.batchParticipantService = batchParticipantService;
        this.regulationService = regulationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public PublicTraceDetailVO getTraceDetail(String token) {
        // 1. 获取扫码基础信息
        PublicQrScanVO scanVO = qrService.scan(token);
        PublicBatchSimpleVO batchSimple = scanVO.getBatch();

        PublicTraceDetailVO detail = new PublicTraceDetailVO();
        
        // 1. 设置核心关联 ID
        detail.setBatchId(batchSimple.getId());
        detail.setQrId(scanVO.getQrId());

        // 2. 映射基础信息
        detail.setProductName(batchSimple.getProductName());
        detail.setCompanyName(batchSimple.getCompanyName());
        detail.setBatchCode(batchSimple.getBatchCode());
        detail.setOriginPlace(batchSimple.getOriginPlace());
        detail.setStartDate(batchSimple.getStartDate());
        detail.setPublicRemark(batchSimple.getPublicRemark());

        // 3. 映射状态
        detail.setBatchStatus(batchSimple.getStatus());
        detail.setRegulationStatus(batchSimple.getRegulationStatus());
        detail.setQrStatus(scanVO.getQrStatus());
        
        // 计算风险提示（参考前端逻辑）
        if ("PENDING_RECTIFY".equals(batchSimple.getRegulationStatus())) {
            detail.setRiskMessage("批次当前处于待整改状态，请谨慎购买。");
        } else if ("RECALLED".equals(batchSimple.getStatus())) {
            detail.setRiskMessage("批次已被召回，请勿食用/使用！");
        }

        // 4. 获取时间轴事件并转换
        List<PublicTraceTimelineVO> timeline = traceEventService.publicTimeline(batchSimple.getId());
        List<PublicTraceDetailVO.PublicEventVO> eventVOs = timeline.stream().map(t -> {
            PublicTraceDetailVO.PublicEventVO ev = new PublicTraceDetailVO.PublicEventVO();
            ev.setStage(t.getStage());
            ev.setTitle(t.getTitle());
            ev.setLocation(t.getLocation());
            if (t.getEventTime() != null) {
                ev.setEventTime(t.getEventTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            // 解析 JSON 并按前端预期结构封装
            try {
                if (t.getContentJson() != null) {
                    Map<String, Object> rawMap = objectMapper.readValue(t.getContentJson(), new TypeReference<Map<String, Object>>() {});
                    
                    Map<String, Object> fieldsMap;
                    // 兼容逻辑：优先提取 fields 内部的数据
                    if (rawMap.containsKey("fields") && rawMap.get("fields") instanceof Map) {
                        fieldsMap = (Map<String, Object>) rawMap.get("fields");
                        // 针对某些可能存在双重嵌套的旧数据进行递归提取，直到取到非单键 fields 的 Map
                        while (fieldsMap.size() == 1 && fieldsMap.containsKey("fields") && fieldsMap.get("fields") instanceof Map) {
                            fieldsMap = (Map<String, Object>) fieldsMap.get("fields");
                        }
                    } else if (rawMap.containsKey("message") && rawMap.size() == 1) {
                        // 针对旧的 {"message": "..."} 结构，将其转换为 {"remark": "..."}
                        fieldsMap = new java.util.HashMap<>();
                        fieldsMap.put("remark", rawMap.get("message"));
                    } else {
                        // 针对新模板录入 (无 fields 包裹) 或其他结构
                        fieldsMap = rawMap;
                    }
                    
                    // 统一适配 trace-web 预期的结构: { "fields": { ... } }
                    // 修正为单层 fields 结构，不再进行双层包装
                    // 注意：前端 trace-web 需要适配单层结构，若之前是 fields.fields 则需修改前端
                    ev.setContent(Map.of("fields", fieldsMap));
                }
            } catch (Exception e) {
                log.error("Parse event contentJson error: {}", t.getContentJson(), e);
            }
            return ev;
        }).collect(Collectors.toList());
        detail.setEvents(eventVOs);

        // 5. 获取公开业务节点并转换为公开版
        detail.setNodes(traceEventService.publicNodeList(batchSimple.getId()).stream().map(n -> {
            PublicTraceDetailVO.PublicNodeVO vo = new PublicTraceDetailVO.PublicNodeVO();
            vo.setTitle(n.getTitle());
            vo.setNodeType(n.getNodeType());
            vo.setBizRole(n.getBizRole());
            vo.setCompanyName(n.getCompanyName());
            
            // 同样对节点内容进行脱敏和解析
            try {
                if (n.getContent() != null) {
                    Map<String, Object> rawMap = objectMapper.readValue(n.getContent(), new TypeReference<Map<String, Object>>() {});
                    Map<String, Object> fieldsMap;
                    // 兼容逻辑：优先提取 fields 内部的数据
                    if (rawMap.containsKey("fields") && rawMap.get("fields") instanceof Map) {
                        fieldsMap = (Map<String, Object>) rawMap.get("fields");
                        // 针对某些可能存在双重嵌套的旧数据进行递归提取，直到取到非单键 fields 的 Map
                        while (fieldsMap.size() == 1 && fieldsMap.containsKey("fields") && fieldsMap.get("fields") instanceof Map) {
                            fieldsMap = (Map<String, Object>) fieldsMap.get("fields");
                        }
                    } else if (rawMap.containsKey("message") && rawMap.size() == 1) {
                        // 针对旧的 {"message": "..."} 结构，将其转换为 {"remark": "..."}
                        fieldsMap = new java.util.HashMap<>();
                        fieldsMap.put("remark", rawMap.get("message"));
                    } else {
                        // 针对新模板录入 (无 fields 包裹) 或其他结构
                        fieldsMap = rawMap;
                    }
                    
                    // 统一适配 trace-web 预期的结构: { "fields": { ... } }
                    // 修正为单层 fields 结构，不再进行双层包装
                    // 注意：前端 trace-web 需要适配单层结构，若之前是 fields.fields 则需修改前端
                    vo.setContent(Map.of("fields", fieldsMap));
                }
            } catch (Exception e) {
                log.error("Parse node content error: {}", n.getContent(), e);
            }

            vo.setEventTime(n.getEventTime());
            return vo;
        }).collect(Collectors.toList()));
        
        // 6. 获取参与企业并转换为公开版
        detail.setParticipants(batchParticipantService.listByBatchId(batchSimple.getId()).stream().map(p -> {
            PublicTraceDetailVO.PublicParticipantVO vo = new PublicTraceDetailVO.PublicParticipantVO();
            vo.setCompanyName(p.getCompanyName());
            vo.setBizRole(p.getBizRole());
            vo.setStageOrder(p.getStageOrder());
            vo.setIsCreator(p.getIsCreator());
            return vo;
        }).collect(Collectors.toList()));

        // 7. 获取质检报告并转换为公开版
        try {
            detail.setQualityReports(qualityReportService.listByBatchId(batchSimple.getId()).stream().map(q -> {
                PublicTraceDetailVO.PublicQualityReportVO vo = new PublicTraceDetailVO.PublicQualityReportVO();
                vo.setReportNo(q.getReportNo());
                vo.setAgency(q.getAgency());
                vo.setResult(q.getResult());
                vo.setReportFileUrl(q.getReportFileUrl());
                vo.setReportDate(q.getCreatedAt());
                return vo;
            }).collect(Collectors.toList()));
        } catch (Exception e) {
            log.warn("Fetch public quality reports failed: {}", e.getMessage());
            detail.setQualityReports(new ArrayList<>());
        }

        // 8. 获取监管记录并转换为公开版
        try {
            detail.setRegulationRecords(regulationService.listPublicByBatchId(batchSimple.getId()).stream().map(r -> {
                PublicTraceDetailVO.PublicRegulationRecordVO vo = new PublicTraceDetailVO.PublicRegulationRecordVO();
                vo.setInspectorName(r.getInspectorName());
                vo.setInspectTime(r.getInspectTime());
                vo.setInspectResult(r.getInspectResult());
                vo.setActionTaken(r.getActionTaken());
                return vo;
            }).collect(Collectors.toList()));
        } catch (Exception e) {
            log.warn("Fetch public regulation records failed: {}", e.getMessage());
            detail.setRegulationRecords(new ArrayList<>());
        }

        // 9. 其他列表暂时返回空数组
        detail.setPesticideRecords(new ArrayList<>());

        return detail;
    }
}
