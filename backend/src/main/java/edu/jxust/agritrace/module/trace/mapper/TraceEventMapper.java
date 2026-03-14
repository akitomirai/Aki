package edu.jxust.agritrace.module.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.trace.vo.PublicTraceTimelineVO;
import edu.jxust.agritrace.module.trace.vo.TraceEventItemVO;
import edu.jxust.agritrace.module.trace.vo.TraceNodeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 溯源事件 Mapper
 */
@Mapper
public interface TraceEventMapper extends BaseMapper<edu.jxust.agritrace.module.trace.entity.TraceEvent> {

    /**
     * 查询后台事件列表
     *
     * @param batchId 批次ID
     * @param stage 阶段
     * @param sourceType 来源类型
     * @return 事件列表
     */
    List<TraceEventItemVO> selectAdminList(@Param("batchId") Long batchId,
                                           @Param("stage") String stage,
                                           @Param("sourceType") String sourceType);

    /**
     * 查询前台时间轴
     *
     * @param batchId 批次ID
     * @return 前台可见事件列表
     */
    List<PublicTraceTimelineVO> selectPublicTimeline(@Param("batchId") Long batchId);

    List<TraceNodeVO> selectNodeList(@Param("batchId") Long batchId,
                                     @Param("publicOnly") Boolean publicOnly);
}
