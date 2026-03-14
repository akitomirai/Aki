package edu.jxust.agritrace.module.company.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("org_company")
public class OrgCompany {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String licenseNo;

    private String address;

    private String contact;

    private String phone;

    private LocalDateTime createdAt;
}
