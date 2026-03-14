package edu.jxust.agritrace.module.regulator.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("regulator_org")
public class RegulatorOrg {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private String address;

    private String contact;

    private String phone;

    private String status;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
