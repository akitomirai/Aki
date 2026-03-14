package edu.jxust.agritrace.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.user.entity.SysUser;
import edu.jxust.agritrace.module.user.dto.SysUserQueryDTO;
import edu.jxust.agritrace.module.user.vo.SysUserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0 LIMIT 1")
    SysUser selectByUsername(String username);

    @Select("SELECT COUNT(1) FROM sys_user WHERE username = #{username} LIMIT 1")
    long countByUsernameAll(@Param("username") String username);

    @Select("""
            <script>
            SELECT
                id,
                username,
                real_name,
                phone,
                role_code,
                company_id,
                regulator_org_id,
                status,
                created_at,
                updated_at
            FROM sys_user
            <where>
                AND deleted = 0
                <if test="dto.username != null and dto.username != ''">
                    AND username LIKE CONCAT('%', #{dto.username}, '%')
                </if>
                <if test="dto.realName != null and dto.realName != ''">
                    AND real_name LIKE CONCAT('%', #{dto.realName}, '%')
                </if>
                <if test="dto.phone != null and dto.phone != ''">
                    AND phone LIKE CONCAT('%', #{dto.phone}, '%')
                </if>
                <if test="dto.roleCode != null and dto.roleCode != ''">
                    AND role_code = #{dto.roleCode}
                </if>
                <if test="dto.status != null">
                    AND status = #{dto.status}
                </if>
                <if test="dto.companyId != null">
                    AND company_id = #{dto.companyId}
                </if>
                <if test="dto.regulatorOrgId != null">
                    AND regulator_org_id = #{dto.regulatorOrgId}
                </if>
            </where>
            ORDER BY created_at DESC, id DESC
            </script>
            """)
    List<SysUserVO> selectList(@Param("dto") SysUserQueryDTO dto);
}
