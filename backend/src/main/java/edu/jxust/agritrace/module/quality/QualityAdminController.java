package edu.jxust.agritrace.module.quality;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.module.quality.dto.QualityReportCreateRequest;
import edu.jxust.agritrace.module.quality.entity.QualityReport;
import edu.jxust.agritrace.module.quality.service.QualityReportService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 质检/用药管理（后台）
 */
@RestController
@RequestMapping("/api/admin/quality")
public class QualityAdminController {

    private final QualityReportService qualityReportService;

    public QualityAdminController(QualityReportService qualityReportService) {
        this.qualityReportService = qualityReportService;
    }

    /**
     * 新增质检报告并生成哈希存证
     */
    @PostMapping("/report")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<QualityReport> createReport(@Valid @RequestBody QualityReportCreateRequest req) {
        QualityReport r = qualityReportService.createWithNotary(req);
        return Result.ok(r);
    }

    /**
     * 重算存证（用于历史数据修复/口径升级）
     */
    @PostMapping("/report/{id}/re-notary")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> reNotary(@PathVariable Long id) {
        boolean updated = qualityReportService.reNotary(id);
        return Result.ok(Map.of("updated", updated));
    }

    /**
     * 查询某批次最新质检校验状态
     */
    @GetMapping("/verify")
    @PreAuthorize("hasAnyRole('ADMIN','REGULATOR','OPERATOR')")
    public Result<?> verify(@RequestParam Long batchId) {
        boolean verified = qualityReportService.verifyLatest(batchId);
        return Result.ok(Map.of("qualityVerified", verified));
    }
}