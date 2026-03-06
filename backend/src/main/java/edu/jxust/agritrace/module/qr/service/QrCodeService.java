package edu.jxust.agritrace.module.qr.service;

import edu.jxust.agritrace.module.qr.dto.QrGenerateRequest;

import java.util.List;
import java.util.Map;

/**
 * 二维码服务
 */
public interface QrCodeService {

    /**
     * 批量生成二维码：返回每个二维码的 id + token
     */
    List<Map<String, Object>> generate(QrGenerateRequest req);
}