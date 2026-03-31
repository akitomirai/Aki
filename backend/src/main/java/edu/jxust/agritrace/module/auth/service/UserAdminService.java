package edu.jxust.agritrace.module.auth.service;

import edu.jxust.agritrace.module.auth.dto.UserCreateRequest;
import edu.jxust.agritrace.module.auth.dto.UserListQueryRequest;
import edu.jxust.agritrace.module.auth.dto.UserResetPasswordRequest;
import edu.jxust.agritrace.module.auth.dto.UserStatusUpdateRequest;
import edu.jxust.agritrace.module.auth.dto.UserUpdateRequest;
import edu.jxust.agritrace.module.auth.vo.UserAdminVO;

import java.util.List;

public interface UserAdminService {

    List<UserAdminVO> listUsers(UserListQueryRequest request);

    UserAdminVO createUser(UserCreateRequest request);

    UserAdminVO updateUser(Long userId, UserUpdateRequest request);

    UserAdminVO updateUserStatus(Long userId, UserStatusUpdateRequest request);

    UserAdminVO resetPassword(Long userId, UserResetPasswordRequest request);
}
