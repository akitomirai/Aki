package edu.jxust.agritrace.module.company.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.company.entity.OrgCompanyBizRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrgCompanyBizRoleMapper extends BaseMapper<OrgCompanyBizRole> {

    @Select("""
            SELECT
                id,
                company_id,
                biz_role,
                created_at,
                updated_at
            FROM org_company_biz_role
            WHERE company_id = #{companyId}
            ORDER BY id ASC
            """)
    List<OrgCompanyBizRole> selectByCompanyId(@Param("companyId") Long companyId);

    @Select("""
            <script>
            SELECT
                id,
                company_id,
                biz_role,
                created_at,
                updated_at
            FROM org_company_biz_role
            WHERE company_id IN
            <foreach collection="companyIds" item="companyId" open="(" separator="," close=")">
                #{companyId}
            </foreach>
            ORDER BY company_id ASC, id ASC
            </script>
            """)
    List<OrgCompanyBizRole> selectByCompanyIds(@Param("companyIds") List<Long> companyIds);
}
