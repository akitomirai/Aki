package edu.jxust.agritrace.module.qr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.qr.entity.QrCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;
import java.util.List;
/**
 * 二维码 Mapper
 */
@Mapper
public interface QrCodeMapper extends BaseMapper<QrCode> {

    /**
     * PV 自增（MySQL 原子更新，先跑通；后面换 Redis 也很容易）
     */
    @Update("UPDATE qr_code SET pv = IFNULL(pv,0) + 1 WHERE id = #{qrId}")
    int incrPv(Long qrId);

    @Select("""
            SELECT id, batch_id, qr_token, status, created_at, expired_at, remark, pv
            FROM qr_code
            WHERE batch_id = #{batchId}
            ORDER BY IFNULL(pv,0) DESC, id DESC
            """)
    List<QrCode> selectByBatchIdOrderByPv(Long batchId);
}