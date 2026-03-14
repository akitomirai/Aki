package edu.jxust.agritrace.controller.enterprise;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.common.log.OperationLogAnnotation;
import edu.jxust.agritrace.module.qr.dto.QrGenerateDTO;
import edu.jxust.agritrace.module.qr.service.QrService;
import edu.jxust.agritrace.module.qr.vo.QrCodeVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * 企业端-二维码管理接口
 */
@Tag(name = "企业端-二维码管理")
@RestController
@RequestMapping("/api/admin/qr")
@PreAuthorize("hasAnyRole('ADMIN','ENTERPRISE_USER')")
public class QrEnterpriseController {

    private final QrService qrService;

    public QrEnterpriseController(QrService qrService) {
        this.qrService = qrService;
    }

    @OperationLogAnnotation(
            module = "QR",
            action = "GENERATE_QR",
            targetType = "QR_CODE"
    )
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

    @OperationLogAnnotation(
            module = "QR",
            action = "DISABLE_QR",
            targetType = "QR_CODE"
    )
    @Operation(summary = "禁用二维码")
    @PutMapping("/disable/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> disable(@PathVariable Long id) {
        qrService.disable(id);
        return Result.ok(true);
    }
}
