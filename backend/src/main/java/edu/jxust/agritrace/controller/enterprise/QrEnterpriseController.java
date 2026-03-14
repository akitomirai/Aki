package edu.jxust.agritrace.controller.enterprise;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.common.log.OperationLogAnnotation;
import edu.jxust.agritrace.common.security.SecurityUtils;
import edu.jxust.agritrace.module.qr.dto.QrGenerateDTO;
import edu.jxust.agritrace.module.qr.service.QrService;
import edu.jxust.agritrace.module.qr.vo.QrCodeVO;
import edu.jxust.agritrace.module.qr.vo.QrDashboardStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "企业端二维码管理")
@RestController
@RequestMapping("/api/admin/qr")
@PreAuthorize("hasAnyRole('ADMIN','ENTERPRISE_USER')")
public class QrEnterpriseController {

    private final QrService qrService;

    public QrEnterpriseController(QrService qrService) {
        this.qrService = qrService;
    }

    @OperationLogAnnotation(module = "QR", action = "GENERATE_QR", targetType = "QR_CODE")
    @Operation(summary = "生成二维码")
    @PostMapping("/generate")
    public Result<Long> generate(@Valid @RequestBody QrGenerateDTO dto) {
        return Result.ok(qrService.generate(dto));
    }

    @Operation(summary = "查询批次二维码列表")
    @GetMapping("/list/{batchId}")
    public Result<List<QrCodeVO>> list(@PathVariable Long batchId) {
        return Result.ok(qrService.listByBatchId(batchId));
    }

    @Operation(summary = "查询二维码详情")
    @GetMapping("/detail/{id}")
    public Result<QrCodeVO> detail(@PathVariable Long id) {
        return Result.ok(qrService.detail(id));
    }

    @Operation(summary = "首页二维码统计总览")
    @GetMapping("/stats/overview")
    public Result<QrDashboardStatsVO> overview() {
        Long companyId = SecurityUtils.getCompanyId();
        if (companyId == null || SecurityUtils.isAdmin()) {
            return Result.ok(qrService.dashboardStatsForPlatform());
        }
        return Result.ok(qrService.dashboardStatsForEnterprise(companyId));
    }

    @OperationLogAnnotation(module = "QR", action = "DISABLE_QR", targetType = "QR_CODE")
    @Operation(summary = "停用二维码")
    @PutMapping("/disable/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> disable(@PathVariable Long id) {
        qrService.disable(id);
        return Result.ok(true);
    }
}
