package edu.jxust.agritrace.common.api.service;

import edu.jxust.agritrace.module.trace.dto.EventCreateRequest;
import edu.jxust.agritrace.module.trace.entity.TraceEvent;

/**
 * 溯源事件服务
 */
public interface TraceEventService {

    /**
     * 新增事件并返回落库后的事件
     */
    TraceEvent create(EventCreateRequest req);
}