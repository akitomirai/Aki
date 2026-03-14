package edu.jxust.agritrace.controller.enterprise;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.common.log.OperationLogAnnotation;
import edu.jxust.agritrace.module.quality.dto.QualityReportCreateDTO;
import edu.jxust.agritrace.module.quality.dto.QualityReportUpdateDTO;
import edu.jxust.agritrace.module.quality.service.QualityReportService;
import edu.jxust.agritrace.module.quality.vo.QualityReportVO;
import edu.jxust.agritrace.module.quality.vo.QualityVerifyVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * 企业端-质检报告管理
 */
@Tag(name = "企业端-质检报告管理")
@RestController
@RequestMapping("/api/admin/quality")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN','REGULATOR','ENTERPRISE_ADMIN','ENTERPRISE_USER','ADMIN')")
public class QualityReportEnterpriseController {

    private final QualityReportService qualityReportService;

    public QualityReportEnterpriseController(QualityReportService qualityReportService) {
        this.qualityReportService = qualityReportService;
    }

    @OperationLogAnnotation(
            module = "QUALITY",
            action = "CREATE_QUALITY_REPORT",
            targetType = "QUALITY_REPORT"
    )
    @Operation(summary = "新增质检报告")
    @PostMapping({"/create", "/report"})
    @PreAuthorize("hasAnyRole('ENTERPRISE_ADMIN','ENTERPRISE_USER','ADMIN')")
    public Result<Long> create(@Valid @RequestBody QualityReportCreateDTO dto) {
        return Result.ok(qualityReportService.create(dto));
    }

    @Operation(summary = "查询批次质检报告列表")
    @GetMapping({"/list/{batchId}", "/report/list/{batchId}"})
    public Result<List<QualityReportVO>> list(@PathVariable Long batchId) {
        return Result.ok(qualityReportService.listByBatchId(batchId));
    }

    @Operation(summary = "查询质检报告详情")
    @GetMapping({"/detail/{id}", "/report/detail/{id}"})
    public Result<QualityReportVO> detail(@PathVariable Long id) {
        return Result.ok(qualityReportService.detail(id));
    }

    @OperationLogAnnotation(
            module = "QUALITY",
            action = "UPDATE_QUALITY_REPORT",
            targetType = "QUALITY_REPORT"
    )
    @Operation(summary = "修改质检报告")
    @PutMapping({"/update", "/report/update"})
    @PreAuthorize("hasAnyRole('ENTERPRISE_ADMIN','ENTERPRISE_USER','ADMIN')")
    public Result<Boolean> update(@Valid @RequestBody QualityReportUpdateDTO dto) {
        qualityReportService.update(dto);
        return Result.ok(true);
    }

    @Operation(summary = "查询批次最新质检结论")
    @GetMapping("/verify")
    public Result<QualityVerifyVO> verify(@RequestParam Long batchId) {
        return Result.ok(qualityReportService.verifyLatestByBatchId(batchId));
    }

    @OperationLogAnnotation(
            module = "QUALITY",
            action = "DELETE_QUALITY_REPORT",
            targetType = "QUALITY_REPORT"
    )
    @Operation(summary = "删除质检报告")
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> delete(@PathVariable Long id) {
        qualityReportService.delete(id);
        return Result.ok(true);
    }
}
