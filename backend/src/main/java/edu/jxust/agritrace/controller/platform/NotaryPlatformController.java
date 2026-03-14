package edu.jxust.agritrace.controller.platform;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.module.notary.service.NotaryVerifyService;
import edu.jxust.agritrace.module.notary.vo.NotaryVerifyVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "平台端-存证校验")
@RestController
@RequestMapping("/api/platform/notary")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class NotaryPlatformController {

    private final NotaryVerifyService notaryVerifyService;

    public NotaryPlatformController(NotaryVerifyService notaryVerifyService) {
        this.notaryVerifyService = notaryVerifyService;
    }

    @Operation(summary = "校验批次存证")
    @GetMapping("/verify/batch/{batchId}")
    public Result<NotaryVerifyVO> verifyBatch(@PathVariable Long batchId) {
        return Result.ok(notaryVerifyService.verifyBatch(batchId));
    }

    @Operation(summary = "校验监管记录存证")
    @GetMapping("/verify/regulation/{recordId}")
    public Result<NotaryVerifyVO> verifyRegulation(@PathVariable Long recordId) {
        return Result.ok(notaryVerifyService.verifyRegulationRecord(recordId));
    }

    @Operation(summary = "校验质检报告存证")
    @GetMapping("/verify/report/{reportId}")
    public Result<NotaryVerifyVO> verifyQualityReport(@PathVariable Long reportId) {
        return Result.ok(notaryVerifyService.verifyQualityReport(reportId));
    }
}