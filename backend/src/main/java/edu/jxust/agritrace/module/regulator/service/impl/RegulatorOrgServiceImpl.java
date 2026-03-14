package edu.jxust.agritrace.module.regulator.service.impl;

import edu.jxust.agritrace.common.exception.BizException;
import edu.jxust.agritrace.module.regulator.dto.RegulatorOrgCreateDTO;
import edu.jxust.agritrace.module.regulator.dto.RegulatorOrgUpdateDTO;
import edu.jxust.agritrace.module.regulator.entity.RegulatorOrg;
import edu.jxust.agritrace.module.regulator.mapper.RegulatorOrgMapper;
import edu.jxust.agritrace.module.regulator.service.RegulatorOrgService;
import edu.jxust.agritrace.module.regulator.vo.RegulatorOrgVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 监管机构管理服务实现类
 */
@Service
public class RegulatorOrgServiceImpl implements RegulatorOrgService {

    private final RegulatorOrgMapper regulatorOrgMapper;

    public RegulatorOrgServiceImpl(RegulatorOrgMapper regulatorOrgMapper) {
        this.regulatorOrgMapper = regulatorOrgMapper;
    }

    /**
     * 创建监管机构
     *
     * @param dto 监管机构创建信息
     * @return 机构ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RegulatorOrgCreateDTO dto) {
        // 监管机构编码唯一性校验
        if (dto.getCode() != null && !dto.getCode().isBlank()) {
            RegulatorOrgVO existing = regulatorOrgMapper.selectByCode(dto.getCode());
            if (existing != null) {
                throw new BizException("监管机构编码已存在");
            }
        }

        RegulatorOrg regulatorOrg = new RegulatorOrg();
        BeanUtils.copyProperties(dto, regulatorOrg);
        regulatorOrg.setStatus("ENABLED");
        regulatorOrg.setCreatedAt(LocalDateTime.now());
        regulatorOrg.setUpdatedAt(LocalDateTime.now());
        regulatorOrgMapper.insert(regulatorOrg);
        return regulatorOrg.getId();
    }

    /**
     * 查询监管机构列表
     *
     * @return 机构列表
     */
    @Override
    public List<RegulatorOrgVO> list() {
        return regulatorOrgMapper.selectList();
    }

    /**
     * 根据ID查询监管机构详情
     *
     * @param id 机构ID
     * @return 机构详情
     */
    @Override
    public RegulatorOrgVO getById(Long id) {
        RegulatorOrg regulatorOrg = regulatorOrgMapper.selectById(id);
        if (regulatorOrg == null) {
            throw new BizException("监管机构信息不存在");
        }
        RegulatorOrgVO vo = new RegulatorOrgVO();
        BeanUtils.copyProperties(regulatorOrg, vo);
        return vo;
    }

    /**
     * 更新监管机构信息
     *
     * @param id 机构ID
     * @param dto 监管机构更新信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RegulatorOrgUpdateDTO dto) {
        RegulatorOrg regulatorOrg = regulatorOrgMapper.selectById(id);
        if (regulatorOrg == null) {
            throw new BizException("监管机构不存在");
        }

        // 唯一性冲突检查
        if (dto.getCode() != null && !dto.getCode().isBlank()) {
            RegulatorOrgVO existing = regulatorOrgMapper.selectByCode(dto.getCode());
            if (existing != null && !existing.getId().equals(id)) {
                throw new BizException("监管机构编码已被其他机构占用");
            }
        }

        BeanUtils.copyProperties(dto, regulatorOrg);
        regulatorOrg.setUpdatedAt(LocalDateTime.now());
        regulatorOrgMapper.updateById(regulatorOrg);
    }
}
