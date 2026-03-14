package edu.jxust.agritrace.module.batch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.batch.entity.TraceBatchParticipant;
import edu.jxust.agritrace.module.batch.vo.BatchParticipantVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TraceBatchParticipantMapper extends BaseMapper<TraceBatchParticipant> {

    @Select("""
            SELECT
                p.id,
                p.batch_id,
                p.company_id,
                c.name AS company_name,
                p.biz_role,
                p.stage_order,
                p.is_creator,
                p.remark
            FROM trace_batch_participant p
            LEFT JOIN org_company c ON p.company_id = c.id
            WHERE p.batch_id = #{batchId}
            ORDER BY
                CASE WHEN p.stage_order IS NULL THEN 1 ELSE 0 END,
                p.stage_order ASC,
                p.id ASC
            """)
    List<BatchParticipantVO> selectByBatchId(@Param("batchId") Long batchId);
}
