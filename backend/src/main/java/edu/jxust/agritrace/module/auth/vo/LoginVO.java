package edu.jxust.agritrace.module.auth.vo;

import lombok.Data;

@Data
public class LoginVO {

    private String token;
    private String tokenType;
    private CurrentUserVO user;
}