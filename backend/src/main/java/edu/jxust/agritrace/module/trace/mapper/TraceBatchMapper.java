package edu.jxust.agritrace.module.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.trace.entity.TraceBatch;
import org.apache.ibatis.annotations.Mapper;

/**
 * 批次 Mapper：用于访问 trace_batch 表。
 */
@Mapper
public interface TraceBatchMapper extends BaseMapper<TraceBatch> {
}