package edu.jxust.agritrace.module.invite.service.impl;

import edu.jxust.agritrace.common.exception.BizException;
import edu.jxust.agritrace.common.security.SecurityUtils;
import edu.jxust.agritrace.module.invite.dto.InviteCodeCreateDTO;
import edu.jxust.agritrace.module.invite.entity.InviteCode;
import edu.jxust.agritrace.module.invite.mapper.InviteCodeMapper;
import edu.jxust.agritrace.module.invite.service.InviteCodeService;
import edu.jxust.agritrace.module.invite.vo.InviteCodeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 邀请码服务实现
 */
@Service
public class InviteCodeServiceImpl implements InviteCodeService {

    private final InviteCodeMapper inviteCodeMapper;
    private final SecureRandom random = new SecureRandom();
    private final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final int CODE_LENGTH = 8;

    public InviteCodeServiceImpl(InviteCodeMapper inviteCodeMapper) {
        this.inviteCodeMapper = inviteCodeMapper;
    }

    /**
     * 生成随机邀请码
     */
    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    /**
     * 创建邀请码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(InviteCodeCreateDTO dto) {
        // 生成唯一邀请码
        String code;
        do {
            code = generateCode();
        } while (inviteCodeMapper.selectByCode(code) != null);

        // 创建邀请码记录
        InviteCode inviteCode = new InviteCode();
        BeanUtils.copyProperties(dto, inviteCode);
        inviteCode.setCode(code);
        inviteCode.setStatus("UNUSED");
        inviteCode.setCreatedBy(SecurityUtils.getUserId());
        inviteCode.setCreatedAt(LocalDateTime.now());
        inviteCode.setUpdatedAt(LocalDateTime.now());

        inviteCodeMapper.insert(inviteCode);
        return code;
    }

    /**
     * 查询所有邀请码列表
     */
    @Override
    public List<InviteCodeVO> list() {
        return inviteCodeMapper.selectAllList();
    }

    /**
     * 根据组织ID查询邀请码列表
     */
    @Override
    public List<InviteCodeVO> listByOrgId(Long orgId) {
        return inviteCodeMapper.selectByOrgId(orgId);
    }

    /**
     * 根据邀请码查询
     */
    @Override
    public InviteCode getByCode(String code) {
        InviteCode inviteCode = inviteCodeMapper.selectByCode(code);
        if (inviteCode == null) {
            throw new BizException("邀请码不存在");
        }
        if (!"UNUSED".equals(inviteCode.getStatus())) {
            throw new BizException("邀请码已使用或已禁用");
        }
        if (inviteCode.getExpireAt() != null && inviteCode.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BizException("邀请码已过期");
        }
        return inviteCode;
    }

    /**
     * 标记邀请码为已使用
     * 使用条件更新防止并发重复使用
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsUsed(Long id, Long userId) {
        int affectedRows = inviteCodeMapper.updateStatusToUsed(id, userId);
        if (affectedRows == 0) {
            throw new BizException("邀请码已使用或已禁用");
        }
    }

    /**
     * 禁用邀请码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        InviteCode inviteCode = inviteCodeMapper.selectById(id);
        if (inviteCode == null) {
            throw new BizException("邀请码不存在");
        }
        inviteCode.setStatus("DISABLED");
        inviteCode.setUpdatedAt(LocalDateTime.now());
        inviteCodeMapper.updateById(inviteCode);
    }

    /**
     * 根据ID查询邀请码详情
     */
    @Override
    public InviteCodeVO getById(Long id) {
        InviteCode inviteCode = inviteCodeMapper.selectById(id);
        if (inviteCode == null) {
            throw new BizException("邀请码不存在");
        }
        InviteCodeVO vo = new InviteCodeVO();
        BeanUtils.copyProperties(inviteCode, vo);
        return vo;
    }
}
