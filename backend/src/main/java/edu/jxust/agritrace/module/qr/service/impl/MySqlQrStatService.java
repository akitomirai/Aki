package edu.jxust.agritrace.module.qr.service.impl;

import edu.jxust.agritrace.module.qr.entity.QrCode;
import edu.jxust.agritrace.module.qr.mapper.QrCodeMapper;
import edu.jxust.agritrace.module.qr.service.QrStatService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * MySQL PV 计数实现：
 * - 适合本地/低并发
 * - 无 Redis 的情况下默认启用
 */
@Service
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class MySqlQrStatService implements QrStatService {

    private final QrCodeMapper qrCodeMapper;

    public MySqlQrStatService(QrCodeMapper qrCodeMapper) {
        this.qrCodeMapper = qrCodeMapper;
    }

    @Override
    public long incrAndGetPv(long qrId) {
        qrCodeMapper.incrPv(qrId);
        QrCode qr = qrCodeMapper.selectById(qrId);
        return qr == null || qr.getPv() == null ? 0 : qr.getPv();
    }
}