package edu.jxust.agritrace.module.qr.dto.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

/**
 * 溯源时间轴响应 DTO：
 * - batch：批次基础信息
 * - events：事件时间轴（按事件时间升序）
 */
@Data
public class TraceTimelineResponse {

    /** 批次信息 */
    private BatchDto batch;

    /** 事件列表 */
    private List<EventDto> events;

    @Data
    public static class BatchDto {
        private Long id;
        private String batchCode;
        private Long productId;
        private Long companyId;
        private String originPlace;
        private String startDate; // 简化为字符串，前端展示方便
        private String status;
        private String remark;
    }

    @Data
    public static class EventDto {
        private Long id;
        private String stage;
        private String eventTime;
        private String location;

        /** 事件内容（JSON对象） */
        private JsonNode content;

        /** 附件（JSON数组，可空） */
        private JsonNode attachments;
    }
}