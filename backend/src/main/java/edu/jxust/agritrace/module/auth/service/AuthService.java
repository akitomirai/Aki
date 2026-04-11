package edu.jxust.agritrace.module.auth.service;

import edu.jxust.agritrace.module.auth.dto.ChangePasswordRequest;
import edu.jxust.agritrace.module.auth.dto.LoginRequest;
import edu.jxust.agritrace.module.auth.dto.UserProfileUpdateRequest;
import edu.jxust.agritrace.module.auth.vo.LoginResponseVO;
import edu.jxust.agritrace.module.auth.vo.LoginUserVO;

public interface AuthService {

    LoginResponseVO login(LoginRequest request);

    LoginUserVO getProfile();

    LoginUserVO updateProfile(UserProfileUpdateRequest request);

    LoginUserVO changePassword(ChangePasswordRequest request);
}
