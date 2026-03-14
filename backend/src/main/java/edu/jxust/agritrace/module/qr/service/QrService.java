package edu.jxust.agritrace.module.qr.service;

import edu.jxust.agritrace.module.qr.dto.QrGenerateDTO;
import edu.jxust.agritrace.module.qr.vo.PublicQrScanVO;
import edu.jxust.agritrace.module.qr.vo.QrCodeVO;

import java.util.List;

/**
 * 二维码服务接口
 */
public interface QrService {

    /**
     * 生成二维码
     *
     * @param dto 生成参数
     * @return 二维码ID
     */
    Long generate(QrGenerateDTO dto);

    /**
     * 查询批次二维码列表
     *
     * @param batchId 批次ID
     * @return 二维码列表
     */
    List<QrCodeVO> listByBatchId(Long batchId);

    List<QrCodeVO> listByBatchIdForPlatform(Long batchId);

    /**
     * 查询二维码详情
     *
     * @param id 二维码ID
     * @return 二维码详情
     */
    QrCodeVO detail(Long id);

    /**
     * 停用二维码
     *
     * @param id 二维码ID
     */
    void disable(Long id);

    /**
     * 扫码查询
     *
     * @param token 二维码 token
     * @return 扫码结果
     */
    PublicQrScanVO scan(String token);
}
