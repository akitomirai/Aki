package edu.jxust.agritrace.module.regulation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.regulation.entity.RegulationRecord;
import edu.jxust.agritrace.module.regulation.vo.RegulationRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 监管记录 Mapper
 */
@Mapper
public interface RegulationRecordMapper extends BaseMapper<RegulationRecord> {

    /**
     * 按批次查询后台监管记录列表
     *
     * @param batchId 批次ID
     * @return 监管记录列表
     */
    @Select("""
            SELECT
                id,
                batch_id,
                inspector_id,
                inspector_name,
                inspect_time,
                inspect_result,
                action_taken,
                remark,
                attachment_url,
                created_at,
                updated_at
            FROM regulation_record
            WHERE batch_id = #{batchId}
            ORDER BY inspect_time DESC, id DESC
            """)
    List<RegulationRecordVO> selectListByBatchId(@Param("batchId") Long batchId);

    /**
     * 按批次查询前台监管记录列表
     *
     * @param batchId 批次ID
     * @return 前台监管记录列表
     */
    @Select("""
            SELECT
                id,
                inspector_name,
                inspect_time,
                inspect_result,
                action_taken,
                remark,
                attachment_url
            FROM regulation_record
            WHERE batch_id = #{batchId}
            ORDER BY inspect_time DESC, id DESC
            """)
    List<RegulationRecordVO> selectPublicListByBatchId(@Param("batchId") Long batchId);
}