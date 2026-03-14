package edu.jxust.agritrace.module.event.service;

import edu.jxust.agritrace.module.event.dto.EventCreateDTO;

/**
 * 事件引擎服务接口
 * 负责统一写入 trace_event
 */
public interface EventEngineService {

    /**
     * 写入通用事件
     *
     * @param dto 事件参数
     * @return 事件ID
     */
    Long createEvent(EventCreateDTO dto);

    /**
     * 写入系统事件
     *
     * @param batchId 批次ID
     * @param title 标题
     * @param contentJson 内容JSON
     * @return 事件ID
     */
    Long createSystemEvent(Long batchId, String title, String contentJson);

    /**
     * 写入监管事件
     *
     * @param batchId 批次ID
     * @param title 标题
     * @param contentJson 内容JSON
     * @return 事件ID
     */
    Long createRegulatorEvent(Long batchId, String title, String contentJson);
}