package edu.jxust.agritrace.module.regulator.service;

import edu.jxust.agritrace.module.regulator.dto.RegulatorOrgCreateDTO;
import edu.jxust.agritrace.module.regulator.dto.RegulatorOrgUpdateDTO;
import edu.jxust.agritrace.module.regulator.vo.RegulatorOrgVO;

import java.util.List;

/**
 * 监管机构管理服务接口
 */
public interface RegulatorOrgService {

    /**
     * 创建监管机构
     *
     * @param dto 创建参数
     * @return 机构ID
     */
    Long create(RegulatorOrgCreateDTO dto);

    /**
     * 查询监管机构列表
     *
     * @return 机构列表
     */
    List<RegulatorOrgVO> list();

    /**
     * 根据ID查询监管机构详情
     *
     * @param id 机构ID
     * @return 机构详情
     */
    RegulatorOrgVO getById(Long id);

    /**
     * 更新监管机构信息
     *
     * @param id 机构ID
     * @param dto 更新参数
     */
    void update(Long id, RegulatorOrgUpdateDTO dto);
}
