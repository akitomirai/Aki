package edu.jxust.agritrace.module.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.trace.entity.TraceEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 溯源事件 Mapper（trace_event）
 */
@Mapper
public interface TraceEventMapper extends BaseMapper<TraceEvent> {

    /**
     * 按时间升序查询某批次的所有事件（用于时间轴展示）
     */
    @Select("""
        SELECT id, batch_id, stage, event_time, operator_id, location,
               content_json AS contentJson, attachments_json AS attachmentsJson, created_at
        FROM trace_event
        WHERE batch_id = #{batchId}
        ORDER BY event_time ASC
    """)
    List<TraceEvent> selectByBatchIdOrderByTime(Long batchId);
}