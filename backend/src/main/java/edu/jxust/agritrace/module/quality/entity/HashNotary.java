package edu.jxust.agritrace.module.quality.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 哈希存证实体（表：hash_notary）
 * 用途：保存业务数据的 SHA-256 指纹，用于校验是否被篡改。
 */
@Data
@TableName("hash_notary")
public class HashNotary {

    private Long id;

    /** 业务类型：QUALITY_REPORT / PESTICIDE_RECORD */
    private String bizType;

    /** 业务主键ID */
    private Long bizId;

    /** SHA-256 十六进制 */
    private String sha256;

    private LocalDateTime createdAt;

    /** 创建人（后续可从 JWT uid 取） */
    private Long createdBy;

    private String remark;
}