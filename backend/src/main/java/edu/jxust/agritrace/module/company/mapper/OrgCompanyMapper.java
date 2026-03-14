package edu.jxust.agritrace.module.company.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.company.entity.OrgCompany;
import edu.jxust.agritrace.module.company.vo.OrgCompanyVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrgCompanyMapper extends BaseMapper<OrgCompany> {

    @Select("""
            SELECT
                id,
                name,
                license_no,
                address,
                contact,
                phone,
                created_at
            FROM org_company
            ORDER BY created_at DESC, id DESC
            """)
    List<OrgCompanyVO> selectList();

    @Select("""
            SELECT
                id,
                name,
                license_no,
                address,
                contact,
                phone,
                created_at
            FROM org_company
            WHERE license_no = #{licenseNo}
            LIMIT 1
            """)
    OrgCompanyVO selectByLicenseNo(@Param("licenseNo") String licenseNo);
}
