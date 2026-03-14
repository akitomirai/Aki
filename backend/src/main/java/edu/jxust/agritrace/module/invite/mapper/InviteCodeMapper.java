package edu.jxust.agritrace.module.invite.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.invite.entity.InviteCode;
import edu.jxust.agritrace.module.invite.vo.InviteCodeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 邀请码 Mapper
 */
@Mapper
public interface InviteCodeMapper extends BaseMapper<InviteCode> {

    /**
     * 查询所有邀请码列表
     *
     * @return 邀请码列表
     */
    @Select("""
            SELECT
                id,
                code,
                invite_type,
                org_type,
                org_id,
                role_code,
                expire_at,
                status,
                used_by,
                used_at,
                created_by,
                remark,
                created_at,
                updated_at
            FROM invite_code
            ORDER BY created_at DESC, id DESC
            """)
    List<InviteCodeVO> selectAllList();

    /**
     * 根据邀请码查询
     *
     * @param code 邀请码
     * @return 邀请码信息
     */
    @Select("""
            SELECT
                id,
                code,
                invite_type,
                org_type,
                org_id,
                role_code,
                expire_at,
                status,
                used_by,
                used_at,
                created_by,
                remark,
                created_at,
                updated_at
            FROM invite_code
            WHERE code = #{code}
            LIMIT 1
            """)
    InviteCode selectByCode(@Param("code") String code);

    /**
     * 根据组织ID查询邀请码列表
     *
     * @param orgId 组织ID
     * @return 邀请码列表
     */
    @Select("""
            SELECT
                id,
                code,
                invite_type,
                org_type,
                org_id,
                role_code,
                expire_at,
                status,
                used_by,
                used_at,
                created_by,
                remark,
                created_at,
                updated_at
            FROM invite_code
            WHERE org_id = #{orgId}
            ORDER BY created_at DESC, id DESC
            """)
    List<InviteCodeVO> selectByOrgId(@Param("orgId") Long orgId);

    /**
     * 条件更新邀请码为已使用
     * 只有当 status = 'UNUSED' 时才更新，防止并发重复使用
     *
     * @param id 邀请码ID
     * @param userId 使用人ID
     * @return 影响行数，0表示邀请码已被使用或已禁用
     */
    @Update("""
            UPDATE invite_code
            SET status = 'USED',
                used_by = #{userId},
                used_at = NOW(),
                updated_at = NOW()
            WHERE id = #{id}
              AND status = 'UNUSED'
            """)
    int updateStatusToUsed(@Param("id") Long id, @Param("userId") Long userId);
}
