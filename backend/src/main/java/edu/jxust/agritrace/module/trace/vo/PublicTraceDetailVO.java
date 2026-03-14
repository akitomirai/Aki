package edu.jxust.agritrace.module.trace.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 前台溯源详情聚合 VO
 * 适配 trace-web 的 TraceDetailView.vue 渲染需求
 */
@Data
public class PublicTraceDetailVO {

    // 核心关联 ID
    private Long batchId;
    private Long qrId;

    // 基础信息
    private String productName;
    private String companyName;
    private String batchCode;
    private String originPlace;
    private LocalDate startDate;
    private String publicRemark;

    // 状态信息
    private String batchStatus;
    private String regulationStatus;
    private String qrStatus;
    private String riskMessage;

    // 动态列表
    private List<PublicEventVO> events;
    private List<PublicNodeVO> nodes;
    private List<PublicQualityReportVO> qualityReports;
    private List<Object> pesticideRecords; // 暂未实现，返回空列表
    private List<PublicRegulationRecordVO> regulationRecords;
    private List<PublicParticipantVO> participants;

    /**
     * 内部事件 VO，将 contentJson 解析为 content 对象
     */
    @Data
    public static class PublicEventVO {
        private String stage;
        private String title;
        private String eventTime;
        private String location;
        private Map<String, Object> content;
    }

    /**
     * 公开版业务节点
     */
    @Data
    public static class PublicNodeVO {
        private String title;
        private String nodeType;
        private String bizRole;
        private String companyName;
        private Object content;
        private LocalDateTime eventTime;
    }

    /**
     * 公开版质检报告
     */
    @Data
    public static class PublicQualityReportVO {
        private String reportNo;
        private String agency;
        private String result;
        private String reportFileUrl;
        private LocalDateTime reportDate;
    }

    /**
     * 公开版监管记录
     */
    @Data
    public static class PublicRegulationRecordVO {
        private String inspectorName;
        private LocalDateTime inspectTime;
        private String inspectResult;
        private String actionTaken;
    }

    /**
     * 公开版参与主体
     */
    @Data
    public static class PublicParticipantVO {
        private String companyName;
        private String bizRole;
        private Integer stageOrder;
        private Boolean isCreator;
    }
}
