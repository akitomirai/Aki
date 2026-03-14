package edu.jxust.agritrace.module.event.service.impl;

import edu.jxust.agritrace.common.exception.BizException;
import edu.jxust.agritrace.module.batch.entity.TraceBatch;
import edu.jxust.agritrace.module.batch.mapper.TraceBatchMapper;
import edu.jxust.agritrace.module.event.dto.EventCreateDTO;
import edu.jxust.agritrace.module.event.service.EventEngineService;
import edu.jxust.agritrace.module.trace.entity.TraceEvent;
import edu.jxust.agritrace.module.trace.mapper.TraceEventMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 事件引擎实现
 * 将各种业务动作统一沉淀到 trace_event
 */
@Service
public class EventEngineServiceImpl implements EventEngineService {

    private final TraceEventMapper traceEventMapper;
    private final TraceBatchMapper traceBatchMapper;

    public EventEngineServiceImpl(TraceEventMapper traceEventMapper,
                                  TraceBatchMapper traceBatchMapper) {
        this.traceEventMapper = traceEventMapper;
        this.traceBatchMapper = traceBatchMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEvent(EventCreateDTO dto) {
        TraceBatch batch = traceBatchMapper.selectById(dto.getBatchId());
        if (batch == null) {
            throw new BizException("批次不存在，无法写入事件");
        }

        TraceEvent event = new TraceEvent();
        event.setBatchId(dto.getBatchId());
        event.setCompanyId(batch.getCompanyId());
        event.setStage(dto.getStage());
        event.setBizRole(deriveBizRole(dto.getStage(), dto.getSourceType()));
        event.setTitle(dto.getTitle());
        event.setEventTime(dto.getEventTime() == null ? LocalDateTime.now() : dto.getEventTime());
        event.setOperatorId(dto.getOperatorId());
        event.setOperatorName(dto.getOperatorName());
        event.setLocation(dto.getLocation());
        event.setSourceType(dto.getSourceType());
        event.setIsPublic(dto.getIsPublic() == null ? true : dto.getIsPublic());
        event.setContentJson(dto.getContentJson());
        event.setAttachmentsJson(dto.getAttachmentsJson() == null ? "[]" : dto.getAttachmentsJson());

        traceEventMapper.insert(event);
        return event.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSystemEvent(Long batchId, String title, String contentJson) {
        EventCreateDTO dto = new EventCreateDTO();
        dto.setBatchId(batchId);
        dto.setStage("SYSTEM");
        dto.setTitle(title);
        dto.setEventTime(LocalDateTime.now());
        dto.setOperatorId(null);
        dto.setOperatorName("系统");
        dto.setLocation(null);
        dto.setSourceType("SYSTEM");
        dto.setIsPublic(true);
        dto.setContentJson(contentJson);
        dto.setAttachmentsJson("[]");
        return createEvent(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRegulatorEvent(Long batchId, String title, String contentJson) {
        EventCreateDTO dto = new EventCreateDTO();
        dto.setBatchId(batchId);
        dto.setStage("INSPECT");
        dto.setTitle(title);
        dto.setEventTime(LocalDateTime.now());
        dto.setOperatorId(null);
        dto.setOperatorName("监管人员");
        dto.setLocation(null);
        dto.setSourceType("REGULATOR");
        dto.setIsPublic(true);
        dto.setContentJson(contentJson);
        dto.setAttachmentsJson("[]");
        return createEvent(dto);
    }

    private String deriveBizRole(String stage, String sourceType) {
        if (stage == null) {
            return null;
        }
        String s = stage.trim().toUpperCase();
        String src = sourceType == null ? "" : sourceType.trim().toUpperCase();
        return switch (s) {
            case "PRODUCE" -> "PRODUCER";
            case "TRANSPORT" -> "TRANSPORTER";
            case "PROCESS" -> "PROCESSOR";
            case "WAREHOUSE" -> "WAREHOUSE";
            case "SALE" -> "SELLER";
            case "INSPECT" -> "REGULATOR".equals(src) ? "REGULATOR" : "INSPECTOR";
            case "SYSTEM" -> "SYSTEM";
            default -> null;
        };
    }
}
