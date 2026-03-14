package edu.jxust.agritrace.module.invite.service;

import edu.jxust.agritrace.module.invite.dto.InviteCodeCreateDTO;
import edu.jxust.agritrace.module.invite.entity.InviteCode;
import edu.jxust.agritrace.module.invite.vo.InviteCodeVO;

import java.util.List;

/**
 * 邀请码服务接口
 */
public interface InviteCodeService {

    /**
     * 创建邀请码
     *
     * @param dto 创建参数
     * @return 邀请码
     */
    String create(InviteCodeCreateDTO dto);

    /**
     * 查询所有邀请码列表
     *
     * @return 邀请码列表
     */
    List<InviteCodeVO> list();

    /**
     * 根据组织ID查询邀请码列表
     *
     * @param orgId 组织ID
     * @return 邀请码列表
     */
    List<InviteCodeVO> listByOrgId(Long orgId);

    /**
     * 根据邀请码查询
     *
     * @param code 邀请码
     * @return 邀请码信息
     */
    InviteCode getByCode(String code);

    /**
     * 标记邀请码为已使用
     *
     * @param id 邀请码ID
     * @param userId 使用人ID
     */
    void markAsUsed(Long id, Long userId);

    /**
     * 禁用邀请码
     *
     * @param id 邀请码ID
     */
    void disable(Long id);

    /**
     * 根据ID查询邀请码详情
     *
     * @param id 邀请码ID
     * @return 邀请码详情
     */
    InviteCodeVO getById(Long id);
}
