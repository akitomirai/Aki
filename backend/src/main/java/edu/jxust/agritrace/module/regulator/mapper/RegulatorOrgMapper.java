package edu.jxust.agritrace.module.regulator.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.regulator.entity.RegulatorOrg;
import edu.jxust.agritrace.module.regulator.vo.RegulatorOrgVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RegulatorOrgMapper extends BaseMapper<RegulatorOrg> {

    @Select("""
            SELECT
                id,
                name,
                code,
                address,
                contact,
                phone,
                status,
                remark,
                created_at,
                updated_at
            FROM regulator_org
            ORDER BY created_at DESC, id DESC
            """)
    List<RegulatorOrgVO> selectList();

    @Select("""
            SELECT
                id,
                name,
                code,
                address,
                contact,
                phone,
                status,
                remark,
                created_at,
                updated_at
            FROM regulator_org
            WHERE code = #{code}
            LIMIT 1
            """)
    RegulatorOrgVO selectByCode(@Param("code") String code);
}
