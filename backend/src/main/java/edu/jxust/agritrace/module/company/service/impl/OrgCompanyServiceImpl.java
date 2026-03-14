package edu.jxust.agritrace.module.company.service.impl;

import edu.jxust.agritrace.common.exception.BizException;
import edu.jxust.agritrace.module.company.dto.OrgCompanyBizRoleUpdateDTO;
import edu.jxust.agritrace.module.company.dto.OrgCompanyCreateDTO;
import edu.jxust.agritrace.module.company.dto.OrgCompanyUpdateDTO;
import edu.jxust.agritrace.module.company.entity.OrgCompany;
import edu.jxust.agritrace.module.company.entity.OrgCompanyBizRole;
import edu.jxust.agritrace.module.company.mapper.OrgCompanyBizRoleMapper;
import edu.jxust.agritrace.module.company.mapper.OrgCompanyMapper;
import edu.jxust.agritrace.module.company.service.OrgCompanyService;
import edu.jxust.agritrace.module.company.vo.OrgCompanyVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 企业管理服务实现类
 */
@Service
public class OrgCompanyServiceImpl implements OrgCompanyService {

    private static final Set<String> ALLOWED_BIZ_ROLES = Set.of(
            "PRODUCER",
            "FARMER",
            "TRANSPORTER",
            "PROCESSOR",
            "SELLER",
            "WAREHOUSE"
    );

    private final OrgCompanyMapper orgCompanyMapper;
    private final OrgCompanyBizRoleMapper orgCompanyBizRoleMapper;

    public OrgCompanyServiceImpl(OrgCompanyMapper orgCompanyMapper,
                                 OrgCompanyBizRoleMapper orgCompanyBizRoleMapper) {
        this.orgCompanyMapper = orgCompanyMapper;
        this.orgCompanyBizRoleMapper = orgCompanyBizRoleMapper;
    }

    /**
     * 创建企业
     *
     * @param dto 企业创建信息
     * @return 企业ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(OrgCompanyCreateDTO dto) {
        // 营业执照号唯一性校验
        if (dto.getLicenseNo() != null && !dto.getLicenseNo().isBlank()) {
            OrgCompanyVO existing = orgCompanyMapper.selectByLicenseNo(dto.getLicenseNo());
            if (existing != null) {
                throw new BizException("营业执照号已存在");
            }
        }

        OrgCompany orgCompany = new OrgCompany();
        BeanUtils.copyProperties(dto, orgCompany);
        orgCompany.setCreatedAt(LocalDateTime.now());
        orgCompanyMapper.insert(orgCompany);
        return orgCompany.getId();
    }

    /**
     * 查询企业列表
     *
     * @return 企业列表
     */
    @Override
    public List<OrgCompanyVO> list() {
        List<OrgCompanyVO> companies = orgCompanyMapper.selectList();
        attachBizRoles(companies);
        return companies;
    }

    /**
     * 根据ID查询企业详情
     *
     * @param id 企业ID
     * @return 企业详情
     */
    @Override
    public OrgCompanyVO getById(Long id) {
        OrgCompany orgCompany = orgCompanyMapper.selectById(id);
        if (orgCompany == null) {
            throw new BizException("企业信息不存在");
        }
        OrgCompanyVO vo = new OrgCompanyVO();
        BeanUtils.copyProperties(orgCompany, vo);
        vo.setBizRoles(getBizRoles(id));
        return vo;
    }

    @Override
    public List<String> getBizRoles(Long id) {
        ensureCompanyExists(id);
        return orgCompanyBizRoleMapper.selectByCompanyId(id)
                .stream()
                .map(OrgCompanyBizRole::getBizRole)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBizRoles(Long id, OrgCompanyBizRoleUpdateDTO dto) {
        ensureCompanyExists(id);

        List<String> rawRoles = dto == null ? null : dto.getBizRoles();
        List<String> normalizedRoles = normalizeBizRoles(rawRoles);

        orgCompanyBizRoleMapper.delete(Wrappers.<OrgCompanyBizRole>lambdaQuery()
                .eq(OrgCompanyBizRole::getCompanyId, id));

        if (normalizedRoles.isEmpty()) {
            return;
        }

        for (String role : normalizedRoles) {
            OrgCompanyBizRole record = new OrgCompanyBizRole();
            record.setCompanyId(id);
            record.setBizRole(role);
            orgCompanyBizRoleMapper.insert(record);
        }
    }

    /**
     * 更新企业信息
     *
     * @param id 企业ID
     * @param dto 企业更新信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, OrgCompanyUpdateDTO dto) {
        OrgCompany orgCompany = ensureCompanyExists(id);

        // 唯一性冲突检查
        if (dto.getLicenseNo() != null && !dto.getLicenseNo().isBlank()) {
            OrgCompanyVO existing = orgCompanyMapper.selectByLicenseNo(dto.getLicenseNo());
            if (existing != null && !existing.getId().equals(id)) {
                throw new BizException("营业执照号已被其他企业占用");
            }
        }

        BeanUtils.copyProperties(dto, orgCompany);
        orgCompanyMapper.updateById(orgCompany);
    }

    private OrgCompany ensureCompanyExists(Long id) {
        OrgCompany orgCompany = orgCompanyMapper.selectById(id);
        if (orgCompany == null) {
            throw new BizException("企业不存在");
        }
        return orgCompany;
    }

    private List<String> normalizeBizRoles(List<String> bizRoles) {
        if (bizRoles == null || bizRoles.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> roles = new LinkedHashSet<>();
        for (String role : bizRoles) {
            if (role == null || role.isBlank()) {
                continue;
            }
            String normalized = role.trim().toUpperCase();
            if (!ALLOWED_BIZ_ROLES.contains(normalized)) {
                throw new BizException("非法业务身份: " + role);
            }
            roles.add(normalized);
        }
        return new ArrayList<>(roles);
    }

    private void attachBizRoles(List<OrgCompanyVO> companies) {
        if (companies == null || companies.isEmpty()) {
            return;
        }
        List<Long> companyIds = companies.stream().map(OrgCompanyVO::getId).toList();
        List<OrgCompanyBizRole> roleRows = orgCompanyBizRoleMapper.selectByCompanyIds(companyIds);
        Map<Long, List<String>> roleMap = roleRows.stream()
                .collect(Collectors.groupingBy(
                        OrgCompanyBizRole::getCompanyId,
                        Collectors.mapping(OrgCompanyBizRole::getBizRole, Collectors.toList())
                ));
        for (OrgCompanyVO company : companies) {
            company.setBizRoles(roleMap.getOrDefault(company.getId(), List.of()));
        }
    }
}
