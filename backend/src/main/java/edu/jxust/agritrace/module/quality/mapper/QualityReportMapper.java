package edu.jxust.agritrace.module.quality.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.quality.entity.QualityReport;
import edu.jxust.agritrace.module.quality.vo.QualityReportVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QualityReportMapper extends BaseMapper<QualityReport> {

    /**
     * 查询批次质检报告
     */
    @Select("""
            SELECT
                id,
                batch_id,
                report_no,
                agency,
                result,
                report_file_url,
                report_json,
                created_at
            FROM quality_report
            WHERE batch_id = #{batchId}
            ORDER BY id DESC
            """)
    List<QualityReportVO> selectByBatchId(@Param("batchId") Long batchId);

    @Select("""
            SELECT
                id,
                batch_id,
                report_no,
                agency,
                result,
                report_file_url,
                report_json,
                created_at
            FROM quality_report
            WHERE batch_id = #{batchId}
            ORDER BY id DESC
            LIMIT 1
            """)
    QualityReportVO selectLatestByBatchId(@Param("batchId") Long batchId);
}
