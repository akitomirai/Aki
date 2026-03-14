package edu.jxust.agritrace.controller.platform;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.module.qr.service.QrService;
import edu.jxust.agritrace.module.qr.vo.QrCodeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "平台端二维码管理")
@RestController
@RequestMapping("/api/platform/qr")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class QrPlatformController {

    private final QrService qrService;

    public QrPlatformController(QrService qrService) {
        this.qrService = qrService;
    }

    @Operation(summary = "查询批次二维码列表")
    @GetMapping("/list/{batchId}")
    public Result<List<QrCodeVO>> list(@PathVariable Long batchId) {
        return Result.ok(qrService.listByBatchIdForPlatform(batchId));
    }
}
