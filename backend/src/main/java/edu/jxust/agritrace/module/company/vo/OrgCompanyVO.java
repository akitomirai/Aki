package edu.jxust.agritrace.module.company.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrgCompanyVO {

    private Long id;

    private String name;

    private String licenseNo;

    private String address;

    private String contact;

    private String phone;

    private List<String> bizRoles;

    private LocalDateTime createdAt;
}
