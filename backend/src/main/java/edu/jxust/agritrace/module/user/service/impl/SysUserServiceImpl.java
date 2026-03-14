package edu.jxust.agritrace.module.user.service.impl;

import edu.jxust.agritrace.common.exception.BizException;
import edu.jxust.agritrace.common.security.SecurityUtils;
import edu.jxust.agritrace.module.user.dto.SysUserQueryDTO;
import edu.jxust.agritrace.module.user.dto.SysUserStatusUpdateDTO;
import edu.jxust.agritrace.module.user.entity.SysUser;
import edu.jxust.agritrace.module.user.mapper.SysUserMapper;
import edu.jxust.agritrace.module.user.service.SysUserService;
import edu.jxust.agritrace.module.user.vo.SysUserVO;
import edu.jxust.agritrace.module.user.vo.SysUserDetailVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户管理服务实现类
 */
@Service
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final String defaultPassword;

    public SysUserServiceImpl(SysUserMapper sysUserMapper,
                              PasswordEncoder passwordEncoder,
                              @Value("${app.user.default-password:}") String defaultPassword) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.defaultPassword = defaultPassword;
    }

    /**
     * 查询用户列表
     *
     * @param dto 查询条件
     * @return 用户列表
     */
    @Override
    public List<SysUserVO> list(SysUserQueryDTO dto) {
        return sysUserMapper.selectList(dto);
    }

    /**
     * 根据ID查询用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @Override
    public SysUserDetailVO getById(Long id) {
        SysUser sysUser = sysUserMapper.selectById(id);
        if (sysUser == null) {
            throw new BizException("用户信息不存在");
        }
        SysUserDetailVO vo = new SysUserDetailVO();
        BeanUtils.copyProperties(sysUser, vo);
        return vo;
    }

    /**
     * 更新用户状态
     *
     * @param id 用户ID
     * @param dto 状态更新参数
     * @param companyId 企业ID
     * @param regulatorOrgId 监管机构ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, SysUserStatusUpdateDTO dto, Long companyId, Long regulatorOrgId) {
        SysUser sysUser = sysUserMapper.selectById(id);
        if (sysUser == null) {
            throw new BizException("用户不存在");
        }

        // 隔离校验
        if (companyId != null && !companyId.equals(sysUser.getCompanyId())) {
            throw new BizException("无权操作该企业用户");
        }
        if (regulatorOrgId != null && !regulatorOrgId.equals(sysUser.getRegulatorOrgId())) {
            throw new BizException("无权操作该监管机构用户");
        }

        // 验证状态值
        if (dto.getStatus() != 0 && dto.getStatus() != 1) {
            throw new BizException("状态值必须为0或1");
        }

        // 禁止禁用平台管理员
        if ("PLATFORM_ADMIN".equals(sysUser.getRoleCode()) && dto.getStatus() == 0) {
            throw new BizException("平台管理员不能被禁用");
        }

        sysUser.setStatus(dto.getStatus());
        sysUserMapper.updateById(sysUser);
    }

    /**
     * 删除用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long companyId, Long regulatorOrgId) {
        if (id != null && id.equals(SecurityUtils.getUserId())) {
            throw new BizException("不能删除自己");
        }

        SysUser sysUser = sysUserMapper.selectById(id);
        if (sysUser == null) {
            throw new BizException("用户不存在或已删除");
        }

        // 隔离校验
        if (companyId != null && !companyId.equals(sysUser.getCompanyId())) {
            throw new BizException("无权删除该企业用户");
        }
        if (regulatorOrgId != null && !regulatorOrgId.equals(sysUser.getRegulatorOrgId())) {
            throw new BizException("无权删除该监管机构用户");
        }

        // 禁止删除平台管理员
        if ("PLATFORM_ADMIN".equals(sysUser.getRoleCode())) {
            throw new BizException("禁止删除平台管理员账号");
        }

        // 最小安全：避免同级管理员互删
        if (!SecurityUtils.isPlatformAdmin()) {
            String targetRoleCode = sysUser.getRoleCode();
            if ("ENTERPRISE_ADMIN".equals(targetRoleCode) || "ADMIN".equals(targetRoleCode) || "REGULATOR".equals(targetRoleCode)) {
                throw new BizException("禁止删除管理员账号");
            }
        }

        sysUserMapper.deleteById(id);
    }

    /**
     * 重置用户密码为系统默认密码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, Long companyId, Long regulatorOrgId) {
        if (id == null) {
            throw new BizException("用户ID不能为空");
        }

        Long operatorUserId = SecurityUtils.getUserId();
        if (id.equals(operatorUserId)) {
            throw new BizException("不能重置自己的密码");
        }

        if (defaultPassword == null || defaultPassword.isBlank()) {
            throw new BizException("系统默认密码未配置，禁止重置");
        }

        SysUser sysUser = sysUserMapper.selectById(id);
        if (sysUser == null) {
            throw new BizException("用户不存在或已删除");
        }

        if (companyId != null && !companyId.equals(sysUser.getCompanyId())) {
            throw new BizException("无权操作该企业用户");
        }
        if (regulatorOrgId != null && !regulatorOrgId.equals(sysUser.getRegulatorOrgId())) {
            throw new BizException("无权操作该监管机构用户");
        }

        if ("PLATFORM_ADMIN".equals(sysUser.getRoleCode())) {
            throw new BizException("禁止重置平台管理员账号密码");
        }

        if (!SecurityUtils.isPlatformAdmin()) {
            String targetRoleCode = sysUser.getRoleCode();
            if ("ENTERPRISE_ADMIN".equals(targetRoleCode)
                    || "ADMIN".equals(targetRoleCode)
                    || "REGULATOR".equals(targetRoleCode)) {
                throw new BizException("禁止重置管理员账号密码");
            }
        }

        sysUser.setPassword(passwordEncoder.encode(defaultPassword));
        sysUserMapper.updateById(sysUser);
    }
}
