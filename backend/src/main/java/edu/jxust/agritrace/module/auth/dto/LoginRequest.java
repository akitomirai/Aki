package edu.jxust.agritrace.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @NotBlank(message = "不能为空")
    @Size(max = 64, message = "长度不能超过 64")
    private String username;

    @NotBlank(message = "不能为空")
    @Size(max = 128, message = "长度不能超过 128")
    private String password;

    @Size(max = 16, message = "长度不能超过 16")
    private String loginChannel;

    private Boolean mobileDevice;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLoginChannel() {
        return loginChannel;
    }

    public void setLoginChannel(String loginChannel) {
        this.loginChannel = loginChannel;
    }

    public Boolean getMobileDevice() {
        return mobileDevice;
    }

    public void setMobileDevice(Boolean mobileDevice) {
        this.mobileDevice = mobileDevice;
    }
}
