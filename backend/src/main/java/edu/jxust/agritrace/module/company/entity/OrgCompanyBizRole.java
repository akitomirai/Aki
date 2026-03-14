package edu.jxust.agritrace.module.company.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("org_company_biz_role")
public class OrgCompanyBizRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long companyId;

    private String bizRole;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
