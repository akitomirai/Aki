package edu.jxust.agritrace.module.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrgCompanyCreateDTO {

    @NotBlank(message = "企业名称不能为空")
    @Size(max = 128, message = "企业名称长度不能超过128")
    private String name;

    @Size(max = 64, message = "营业执照号长度不能超过64")
    private String licenseNo;

    @Size(max = 255, message = "地址长度不能超过255")
    private String address;

    @Size(max = 64, message = "联系人长度不能超过64")
    private String contact;

    @Size(max = 32, message = "联系电话长度不能超过32")
    private String phone;
}
