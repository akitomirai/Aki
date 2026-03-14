package edu.jxust.agritrace.module.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SysUserStatusUpdateDTO {

    @NotNull(message = "状态不能为空")
    private Integer status;
}
