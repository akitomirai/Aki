package edu.jxust.agritrace.module.qr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.qr.entity.QrCode;
import edu.jxust.agritrace.module.qr.vo.QrCodeVO;
import edu.jxust.agritrace.module.qr.vo.PublicBatchSimpleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 二维码 Mapper
 */
@Mapper
public interface QrCodeMapper extends BaseMapper<QrCode> {

    /**
     * 查询批次二维码列表
     *
     * @param batchId 批次ID
     * @return 二维码列表
     */
    List<QrCodeVO> selectByBatchId(@Param("batchId") Long batchId);

    Long countByCompanyId(@Param("companyId") Long companyId);

    /**
     * 根据 token 查询二维码
     *
     * @param token 二维码 token
     * @return 二维码
     */
    QrCode selectByToken(@Param("token") String token);

    /**
     * 查询前台扫码对应的批次简要信息
     *
     * @param batchId 批次ID
     * @return 批次简要信息
     */
    PublicBatchSimpleVO selectPublicBatchSimple(@Param("batchId") Long batchId);
}
