package edu.jxust.agritrace.controller.platform;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.common.log.OperationLogAnnotation;
import edu.jxust.agritrace.module.company.dto.OrgCompanyCreateDTO;
import edu.jxust.agritrace.module.company.dto.OrgCompanyBizRoleUpdateDTO;
import edu.jxust.agritrace.module.company.dto.OrgCompanyUpdateDTO;
import edu.jxust.agritrace.module.company.service.OrgCompanyService;
import edu.jxust.agritrace.module.company.vo.OrgCompanyVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * 平台端-企业管理控制器
 */
@Tag(name = "平台端-企业管理")
@RestController
@RequestMapping("/api/platform/company")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class OrgCompanyPlatformController {

    private final OrgCompanyService orgCompanyService;

    public OrgCompanyPlatformController(OrgCompanyService orgCompanyService) {
        this.orgCompanyService = orgCompanyService;
    }

    /**
     * 新增企业
     *
     * @param dto 企业创建信息
     * @return 企业ID
     */
    @OperationLogAnnotation(
            module = "COMPANY",
            action = "CREATE_COMPANY",
            targetType = "COMPANY"
    )
    @Operation(summary = "新增企业")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody OrgCompanyCreateDTO dto) {
        Long id = orgCompanyService.create(dto);
        return Result.ok(id);
    }

    /**
     * 查询企业列表
     *
     * @return 企业列表
     */
    @Operation(summary = "查询企业列表")
    @GetMapping("/list")
    public Result<List<OrgCompanyVO>> list() {
        return Result.ok(orgCompanyService.list());
    }

    /**
     * 查询企业详情
     *
     * @param id 企业ID
     * @return 企业详情
     */
    @Operation(summary = "查询企业详情")
    @GetMapping("/detail/{id}")
    public Result<OrgCompanyVO> detail(@Parameter(description = "企业ID") @PathVariable Long id) {
        return Result.ok(orgCompanyService.getById(id));
    }

    @Operation(summary = "查询企业业务身份")
    @GetMapping("/{id}/biz-roles")
    public Result<List<String>> bizRoles(@Parameter(description = "企业ID") @PathVariable Long id) {
        return Result.ok(orgCompanyService.getBizRoles(id));
    }

    /**
     * 修改企业
     *
     * @param id 企业ID
     * @param dto 企业修改信息
     * @return 成功
     */
    @OperationLogAnnotation(
            module = "COMPANY",
            action = "UPDATE_COMPANY",
            targetType = "COMPANY"
    )
    @Operation(summary = "修改企业")
    @PutMapping("/{id}")
    public Result<Void> update(
            @Parameter(description = "企业ID") @PathVariable Long id,
            @Valid @RequestBody OrgCompanyUpdateDTO dto) {
        orgCompanyService.update(id, dto);
        return Result.ok();
    }

    @OperationLogAnnotation(
            module = "COMPANY",
            action = "UPDATE_COMPANY_BIZ_ROLES",
            targetType = "COMPANY"
    )
    @Operation(summary = "更新企业业务身份")
    @PutMapping("/{id}/biz-roles")
    public Result<Void> updateBizRoles(
            @Parameter(description = "企业ID") @PathVariable Long id,
            @RequestBody OrgCompanyBizRoleUpdateDTO dto) {
        orgCompanyService.updateBizRoles(id, dto);
        return Result.ok();
    }
}
