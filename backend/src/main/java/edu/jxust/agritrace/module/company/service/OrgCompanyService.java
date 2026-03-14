package edu.jxust.agritrace.module.company.service;

import edu.jxust.agritrace.module.company.dto.OrgCompanyCreateDTO;
import edu.jxust.agritrace.module.company.dto.OrgCompanyBizRoleUpdateDTO;
import edu.jxust.agritrace.module.company.dto.OrgCompanyUpdateDTO;
import edu.jxust.agritrace.module.company.vo.OrgCompanyVO;

import java.util.List;

/**
 * 企业管理服务接口
 */
public interface OrgCompanyService {

    /**
     * 创建企业
     *
     * @param dto 创建参数
     * @return 企业ID
     */
    Long create(OrgCompanyCreateDTO dto);

    /**
     * 查询企业列表
     *
     * @return 企业列表
     */
    List<OrgCompanyVO> list();

    /**
     * 根据ID查询企业详情
     *
     * @param id 企业ID
     * @return 企业详情
     */
    OrgCompanyVO getById(Long id);

    List<String> getBizRoles(Long id);

    void updateBizRoles(Long id, OrgCompanyBizRoleUpdateDTO dto);

    /**
     * 更新企业信息
     *
     * @param id 企业ID
     * @param dto 更新参数
     */
    void update(Long id, OrgCompanyUpdateDTO dto);
}
