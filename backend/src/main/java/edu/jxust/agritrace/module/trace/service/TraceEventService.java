package edu.jxust.agritrace.module.trace.service;

import edu.jxust.agritrace.module.trace.dto.TraceEventCreateDTO;
import edu.jxust.agritrace.module.trace.dto.TraceEventListQueryDTO;
import edu.jxust.agritrace.module.trace.vo.PublicTraceTimelineVO;
import edu.jxust.agritrace.module.trace.vo.TraceEventItemVO;
import edu.jxust.agritrace.module.trace.vo.TraceNodeVO;

import java.util.List;

/**
 * 溯源事件服务接口
 */
public interface TraceEventService {

    /**
     * 新增溯源事件
     *
     * @param dto 新增参数
     * @return 事件ID
     */
    Long create(TraceEventCreateDTO dto);

    /**
     * 查询后台事件列表
     *
     * @param dto 查询参数
     * @return 事件列表
     */
    List<TraceEventItemVO> list(TraceEventListQueryDTO dto);

    /**
     * 删除事件
     *
     * @param id 事件ID
     */
    void delete(Long id);

    /**
     * 查询前台时间轴
     *
     * @param batchId 批次ID
     * @return 时间轴列表
     */
    List<PublicTraceTimelineVO> publicTimeline(Long batchId);

    List<TraceNodeVO> nodeList(Long batchId);

    List<TraceNodeVO> publicNodeList(Long batchId);
}
