package edu.jxust.agritrace.module.qr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.qr.entity.QrQueryLog;
import org.apache.ibatis.annotations.Mapper;
import edu.jxust.agritrace.module.qr.dto.QrPvTrendPoint;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 查询日志 Mapper（qr_query_log）
 */
@Mapper
public interface QrQueryLogMapper extends BaseMapper<QrQueryLog> {

    @Select("""
            SELECT DATE(query_time) AS day, COUNT(*) AS pv
            FROM qr_query_log
            WHERE qr_id = #{qrId}
            AND query_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)
            GROUP BY DATE(query_time)
            ORDER BY day ASC
            """)
    List<QrPvTrendPoint> selectPvTrend(Long qrId, int days);
}