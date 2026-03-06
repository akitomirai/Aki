package edu.jxust.agritrace.module.qr.service.impl;

import edu.jxust.agritrace.common.util.QrTokenUtil;
import edu.jxust.agritrace.module.qr.dto.QrGenerateRequest;
import edu.jxust.agritrace.module.qr.entity.QrCode;
import edu.jxust.agritrace.module.qr.mapper.QrCodeMapper;
import edu.jxust.agritrace.module.qr.service.QrCodeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 二维码服务实现：
 * 1) 先 insert，拿到自增 id（qrId）
 * 2) 用 qrId + expireAt 生成 token
 * 3) 回写 token
 */
@Service
public class QrCodeServiceImpl implements QrCodeService {

    private final QrCodeMapper qrCodeMapper;

    @Value("${app.qr.hmac-secret}")
    private String qrSecret;

    public QrCodeServiceImpl(QrCodeMapper qrCodeMapper) {
        this.qrCodeMapper = qrCodeMapper;
    }

    @Override
    @Transactional
    public List<Map<String, Object>> generate(QrGenerateRequest req) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (int i = 0; i < req.getCount(); i++) {
            QrCode qr = new QrCode();
            qr.setBatchId(req.getBatchId());
            qr.setStatus("ACTIVE");
            qr.setRemark(req.getRemark());
            qr.setPv(0L);

            // expiredAt：可选
            LocalDateTime expiredAt = null;
            long expEpoch;
            if (req.getExpireDays() > 0) {
                expiredAt = LocalDateTime.now().plusDays(req.getExpireDays());
                expEpoch = expiredAt.atZone(ZoneId.systemDefault()).toEpochSecond();
            } else {
                // 不过期：给个很远的未来时间
                expEpoch = Instant.now().plusSeconds(3650L * 24 * 3600).getEpochSecond();
            }
            qr.setExpiredAt(expiredAt);

            // 1) insert 获取 qrId
            qrCodeMapper.insert(qr);

            // 2) 生成 token
            String token = QrTokenUtil.generate(qr.getId(), expEpoch, qrSecret);

            // 3) 回写 token
            qr.setQrToken(token);
            qrCodeMapper.updateById(qr);

            result.add(Map.of(
                    "qrId", qr.getId(),
                    "qrToken", token
            ));
        }

        return result;
    }
}