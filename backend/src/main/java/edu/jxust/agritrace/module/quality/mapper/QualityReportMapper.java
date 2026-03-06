package edu.jxust.agritrace.module.quality.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.quality.entity.QualityReport;
import org.apache.ibatis.annotations.Mapper;

/**
 * 质检报告 Mapper
 */
@Mapper
public interface QualityReportMapper extends BaseMapper<QualityReport> {
}