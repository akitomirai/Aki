package edu.jxust.agritrace.module.notary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("hash_notary")
public class HashNotary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizType;

    private Long bizId;

    private String sha256;

    private LocalDateTime createdAt;

    private Long createdBy;

    private String remark;
}