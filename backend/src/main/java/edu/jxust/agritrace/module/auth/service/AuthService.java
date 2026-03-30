package edu.jxust.agritrace.module.auth.service;

import edu.jxust.agritrace.module.auth.dto.LoginRequest;
import edu.jxust.agritrace.module.auth.vo.LoginResponseVO;

public interface AuthService {

    LoginResponseVO login(LoginRequest request);
}
