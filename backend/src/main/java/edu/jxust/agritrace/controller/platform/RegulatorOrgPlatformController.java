package edu.jxust.agritrace.controller.platform;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.common.log.OperationLogAnnotation;
import edu.jxust.agritrace.module.regulator.dto.RegulatorOrgCreateDTO;
import edu.jxust.agritrace.module.regulator.dto.RegulatorOrgUpdateDTO;
import edu.jxust.agritrace.module.regulator.service.RegulatorOrgService;
import edu.jxust.agritrace.module.regulator.vo.RegulatorOrgVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * 平台端-监管机构管理控制器
 */
@Tag(name = "平台端-监管机构管理")
@RestController
@RequestMapping("/api/platform/regulator-org")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class RegulatorOrgPlatformController {

    private final RegulatorOrgService regulatorOrgService;

    public RegulatorOrgPlatformController(RegulatorOrgService regulatorOrgService) {
        this.regulatorOrgService = regulatorOrgService;
    }

    /**
     * 新增监管机构
     *
     * @param dto 监管机构创建信息
     * @return 监管机构ID
     */
    @OperationLogAnnotation(
            module = "REGULATOR",
            action = "CREATE_REGULATOR",
            targetType = "REGULATOR"
    )
    @Operation(summary = "新增监管机构")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody RegulatorOrgCreateDTO dto) {
        Long id = regulatorOrgService.create(dto);
        return Result.ok(id);
    }

    /**
     * 查询监管机构列表
     *
     * @return 监管机构列表
     */
    @Operation(summary = "查询监管机构列表")
    @GetMapping("/list")
    public Result<List<RegulatorOrgVO>> list() {
        return Result.ok(regulatorOrgService.list());
    }

    /**
     * 查询监管机构详情
     *
     * @param id 监管机构ID
     * @return 监管机构详情
     */
    @Operation(summary = "查询监管机构详情")
    @GetMapping("/detail/{id}")
    public Result<RegulatorOrgVO> detail(@Parameter(description = "监管机构ID") @PathVariable Long id) {
        return Result.ok(regulatorOrgService.getById(id));
    }

    /**
     * 修改监管机构
     *
     * @param id 监管机构ID
     * @param dto 监管机构修改信息
     * @return 成功
     */
    @OperationLogAnnotation(
            module = "REGULATOR",
            action = "UPDATE_REGULATOR",
            targetType = "REGULATOR"
    )
    @Operation(summary = "修改监管机构")
    @PutMapping("/{id}")
    public Result<Void> update(
            @Parameter(description = "监管机构ID") @PathVariable Long id,
            @Valid @RequestBody RegulatorOrgUpdateDTO dto) {
        regulatorOrgService.update(id, dto);
        return Result.ok();
    }
}
