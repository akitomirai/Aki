package edu.jxust.agritrace.common.api.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jxust.agritrace.module.trace.dto.EventCreateRequest;
import edu.jxust.agritrace.module.trace.entity.TraceEvent;
import edu.jxust.agritrace.module.trace.mapper.TraceEventMapper;
import edu.jxust.agritrace.common.api.service.TraceEventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 溯源事件服务实现（方案B）
 * - 接收 JsonNode
 * - 使用 ObjectMapper 转为字符串写入 MySQL JSON 字段
 */
@Service
public class TraceEventServiceImpl implements TraceEventService {

    private final TraceEventMapper traceEventMapper;
    private final ObjectMapper objectMapper;

    public TraceEventServiceImpl(TraceEventMapper traceEventMapper, ObjectMapper objectMapper) {
        this.traceEventMapper = traceEventMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public TraceEvent create(EventCreateRequest req) {
        TraceEvent e = new TraceEvent();
        e.setBatchId(req.getBatchId());
        e.setStage(req.getStage());
        e.setEventTime(req.getEventTime() == null ? LocalDateTime.now() : req.getEventTime());
        e.setLocation(req.getLocation());

        // operatorId：后面我们从 JWT 的 uid 中解析写入
        e.setOperatorId(null);

        // JsonNode -> JSON 字符串
        e.setContentJson(toJson(req.getContent()));
        e.setAttachmentsJson(req.getAttachments() == null ? null : toJson(req.getAttachments()));

        traceEventMapper.insert(e);
        return e;
    }

    /**
     * 将 JsonNode 安全序列化为 JSON 字符串。
     */
    private String toJson(Object node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            // 这里抛运行时异常即可，后续我们再加全局异常处理统一返回
            throw new IllegalArgumentException("JSON序列化失败: " + ex.getMessage(), ex);
        }
    }
}