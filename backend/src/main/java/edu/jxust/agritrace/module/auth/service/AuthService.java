package edu.jxust.agritrace.module.auth.service;

import edu.jxust.agritrace.module.auth.dto.LoginDTO;
import edu.jxust.agritrace.module.auth.vo.CurrentUserVO;
import edu.jxust.agritrace.module.auth.vo.LoginVO;

public interface AuthService {

    LoginVO login(LoginDTO dto);

    CurrentUserVO getCurrentUser();

    void logout();

    /**
     * 用户注册
     *
     * @param dto 注册参数
     */
    void register(edu.jxust.agritrace.module.auth.dto.RegisterDTO dto);
}