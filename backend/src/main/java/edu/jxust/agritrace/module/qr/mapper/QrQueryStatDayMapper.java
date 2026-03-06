package edu.jxust.agritrace.module.qr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.qr.dto.QrPvTrendPoint;
import edu.jxust.agritrace.module.qr.entity.QrQueryStatDay;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 日统计 Mapper
 */
@Mapper
public interface QrQueryStatDayMapper extends BaseMapper<QrQueryStatDay> {

    /**
     * 趋势查询（已有）
     */
    @Select("""
      SELECT DATE_FORMAT(day, '%Y-%m-%d') AS day, pv
      FROM qr_query_stat_day
      WHERE qr_id = #{qrId}
        AND day >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)
      ORDER BY day ASC
    """)
    List<QrPvTrendPoint> selectPvTrendFromStat(Long qrId, int days);

    /**
     * 查询某个二维码最近一次的 UV（用于详情页列表展示）
     */
    @Select("""
      SELECT uv
      FROM qr_query_stat_day
      WHERE qr_id = #{qrId}
      ORDER BY day DESC
      LIMIT 1
    """)
    Long selectLatestUv(Long qrId);
}