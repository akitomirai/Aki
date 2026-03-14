package edu.jxust.agritrace.module.user.service;

import edu.jxust.agritrace.module.user.dto.SysUserQueryDTO;
import edu.jxust.agritrace.module.user.dto.SysUserStatusUpdateDTO;
import edu.jxust.agritrace.module.user.vo.SysUserVO;
import edu.jxust.agritrace.module.user.vo.SysUserDetailVO;

import java.util.List;

/**
 * 用户管理服务接口
 */
public interface SysUserService {

    /**
     * 查询用户列表
     *
     * @param dto 查询条件
     * @return 用户列表
     */
    List<SysUserVO> list(SysUserQueryDTO dto);

    /**
     * 根据ID查询用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    SysUserDetailVO getById(Long id);

    /**
     * 更新用户状态
     *
     * @param id 用户ID
     * @param dto 状态更新参数
     * @param companyId 企业ID (隔离用，null表示不限制)
     * @param regulatorOrgId 监管机构ID (隔离用，null表示不限制)
     */
    void updateStatus(Long id, SysUserStatusUpdateDTO dto, Long companyId, Long regulatorOrgId);
    /**
     * 删除用户
     *
     * @param id 用户ID
     * @param companyId 企业ID
     * @param regulatorOrgId 监管机构ID
     */
    void delete(Long id, Long companyId, Long regulatorOrgId);

    /**
     * 重置用户密码为系统默认密码
     *
     * @param id 用户ID
     * @param companyId 企业ID (隔离用，null表示不限制)
     * @param regulatorOrgId 监管机构ID (隔离用，null表示不限制)
     */
    void resetPassword(Long id, Long companyId, Long regulatorOrgId);
}
