package edu.jxust.agritrace.module.notary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jxust.agritrace.module.notary.entity.HashNotary;
import edu.jxust.agritrace.module.notary.mapper.HashNotaryMapper;
import edu.jxust.agritrace.module.notary.service.HashNotaryService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Service
public class HashNotaryServiceImpl implements HashNotaryService {

    private final HashNotaryMapper hashNotaryMapper;

    public HashNotaryServiceImpl(HashNotaryMapper hashNotaryMapper) {
        this.hashNotaryMapper = hashNotaryMapper;
    }

    @Override
    public void notarize(String bizType, Long bizId, String snapshot, Long operatorId, String remark) {
        String digest = sha256(snapshot);

        HashNotary existing = hashNotaryMapper.selectOne(
                new LambdaQueryWrapper<HashNotary>()
                        .eq(HashNotary::getBizType, bizType)
                        .eq(HashNotary::getBizId, bizId)
        );

        if (existing == null) {
            HashNotary entity = new HashNotary();
            entity.setBizType(bizType);
            entity.setBizId(bizId);
            entity.setSha256(digest);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setCreatedBy(operatorId);
            entity.setRemark(remark);
            hashNotaryMapper.insert(entity);
            return;
        }

        existing.setSha256(digest);
        existing.setCreatedAt(LocalDateTime.now());
        existing.setCreatedBy(operatorId);
        existing.setRemark(remark);
        hashNotaryMapper.updateById(existing);
    }

    @Override
    public String sha256(String content) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = messageDigest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() < 2) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("计算SHA-256失败", e);
        }
    }
}