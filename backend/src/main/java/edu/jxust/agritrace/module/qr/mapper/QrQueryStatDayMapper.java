package edu.jxust.agritrace.module.qr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.qr.entity.QrQueryStatDay;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 扫码日统计 Mapper
 */
@Mapper
public interface QrQueryStatDayMapper extends BaseMapper<QrQueryStatDay> {

    /**
     * 按二维码和日期查询统计
     *
     * @param qrId 二维码ID
     * @param day 日期
     * @return 日统计
     */
    @Select("SELECT * FROM qr_query_stat_day WHERE qr_id = #{qrId} AND day = #{day} LIMIT 1")
    QrQueryStatDay selectByQrIdAndDay(@Param("qrId") Long qrId, @Param("day") java.time.LocalDate day);
}