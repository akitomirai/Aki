package edu.jxust.agritrace.module.regulator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegulatorOrgCreateDTO {

    @NotBlank(message = "监管机构名称不能为空")
    @Size(max = 128, message = "监管机构名称长度不能超过128")
    private String name;

    @NotBlank(message = "监管机构编码不能为空")
    @Size(max = 64, message = "监管机构编码长度不能超过64")
    private String code;

    @Size(max = 255, message = "地址长度不能超过255")
    private String address;

    @Size(max = 64, message = "联系人长度不能超过64")
    private String contact;

    @Size(max = 32, message = "联系电话长度不能超过32")
    private String phone;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
