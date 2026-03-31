package edu.jxust.agritrace.controller;

import edu.jxust.agritrace.common.api.ApiResponse;
import edu.jxust.agritrace.module.batch.dto.BatchAssignmentRequest;
import edu.jxust.agritrace.module.batch.dto.BatchCreateRequest;
import edu.jxust.agritrace.module.batch.dto.BatchListQueryRequest;
import edu.jxust.agritrace.module.batch.dto.BatchRiskActionCreateRequest;
import edu.jxust.agritrace.module.batch.dto.BatchStatusActionRequest;
import edu.jxust.agritrace.module.batch.dto.BatchUpdateRequest;
import edu.jxust.agritrace.module.batch.dto.FieldDraftSaveRequest;
import edu.jxust.agritrace.module.batch.dto.QualityReportCreateRequest;
import edu.jxust.agritrace.module.batch.dto.TraceRecordCreateRequest;
import edu.jxust.agritrace.module.batch.service.BatchService;
import edu.jxust.agritrace.module.batch.vo.AttachmentCleanupResultVO;
import edu.jxust.agritrace.module.batch.vo.BatchListItemVO;
import edu.jxust.agritrace.module.batch.vo.BatchWorkbenchVO;
import edu.jxust.agritrace.module.batch.vo.CompanyOptionVO;
import edu.jxust.agritrace.module.batch.vo.FieldDraftVO;
import edu.jxust.agritrace.module.batch.vo.FileAssetVO;
import edu.jxust.agritrace.module.batch.vo.OperatorOptionVO;
import edu.jxust.agritrace.module.batch.vo.ProductOptionVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @GetMapping
    public ApiResponse<List<BatchListItemVO>> listBatches(@ModelAttribute BatchListQueryRequest request) {
        return ApiResponse.ok(batchService.listBatches(request));
    }

    @GetMapping("/lookup/companies")
    public ApiResponse<List<CompanyOptionVO>> listCompanyOptions(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(batchService.listCompanyOptions(keyword));
    }

    @GetMapping("/lookup/products")
    public ApiResponse<List<ProductOptionVO>> listProductOptions(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(batchService.listProductOptions(companyId, keyword));
    }

    @GetMapping("/lookup/operators")
    public ApiResponse<List<OperatorOptionVO>> listOperatorOptions(@RequestParam(required = false) Long companyId) {
        return ApiResponse.ok(batchService.listAssignableOperators(companyId));
    }

    @PostMapping("/files/upload")
    public ApiResponse<List<FileAssetVO>> uploadFiles(
            @RequestParam String businessType,
            @RequestPart("files") List<MultipartFile> files
    ) {
        return ApiResponse.ok(batchService.uploadAttachments(businessType, files));
    }

    @PostMapping("/files/cleanup")
    public ApiResponse<AttachmentCleanupResultVO> cleanupOrphanFiles() {
        AttachmentCleanupResultVO result = batchService.cleanupExpiredOrphanAttachments();
        return ApiResponse.ok("附件清理已完成。", result);
    }

    @GetMapping("/field-drafts")
    public ApiResponse<List<FieldDraftVO>> listMyFieldDrafts() {
        return ApiResponse.ok(batchService.listMyFieldDrafts());
    }

    @GetMapping("/{batchId}/field-draft")
    public ApiResponse<FieldDraftVO> getMyFieldDraft(@PathVariable Long batchId) {
        return ApiResponse.ok(batchService.getMyFieldDraft(batchId));
    }

    @PostMapping("/{batchId}/field-draft")
    public ApiResponse<FieldDraftVO> saveFieldDraft(@PathVariable Long batchId, @RequestBody FieldDraftSaveRequest request) {
        return ApiResponse.ok("草稿已保存。", batchService.saveFieldDraft(batchId, request));
    }

    @DeleteMapping("/{batchId}/field-draft")
    public ApiResponse<Void> deleteFieldDraft(@PathVariable Long batchId) {
        batchService.deleteFieldDraft(batchId);
        return ApiResponse.ok("草稿已删除。", null);
    }

    @GetMapping("/{batchId}")
    public ApiResponse<BatchWorkbenchVO> getBatchWorkbench(@PathVariable Long batchId) {
        return ApiResponse.ok(batchService.getBatchWorkbench(batchId));
    }

    @PostMapping
    public ApiResponse<BatchWorkbenchVO> createBatch(@Valid @RequestBody BatchCreateRequest request) {
        return ApiResponse.ok("批次已创建。", batchService.createBatch(request));
    }

    @PatchMapping("/{batchId}")
    public ApiResponse<BatchWorkbenchVO> updateBatch(@PathVariable Long batchId, @Valid @RequestBody BatchUpdateRequest request) {
        return ApiResponse.ok("批次资料已更新。", batchService.updateBatch(batchId, request));
    }

    @PostMapping("/{batchId}/status")
    public ApiResponse<BatchWorkbenchVO> changeStatus(@PathVariable Long batchId, @Valid @RequestBody BatchStatusActionRequest request) {
        return ApiResponse.ok("批次状态已更新。", batchService.changeStatus(batchId, request));
    }

    @PostMapping("/{batchId}/records")
    public ApiResponse<BatchWorkbenchVO> addTraceRecord(@PathVariable Long batchId, @Valid @RequestBody TraceRecordCreateRequest request) {
        return ApiResponse.ok("追溯记录已补充。", batchService.addTraceRecord(batchId, request));
    }

    @PostMapping("/{batchId}/records/quick")
    public ApiResponse<BatchWorkbenchVO> addQuickTraceRecord(@PathVariable Long batchId, @Valid @RequestBody TraceRecordCreateRequest request) {
        return ApiResponse.ok("追溯记录已补充。", batchService.addTraceRecord(batchId, request));
    }

    @PostMapping("/{batchId}/assignment")
    public ApiResponse<BatchWorkbenchVO> assignBatchOperator(@PathVariable Long batchId, @RequestBody BatchAssignmentRequest request) {
        return ApiResponse.ok("批次分配已更新。", batchService.assignBatchOperator(batchId, request));
    }

    @PostMapping("/{batchId}/quality-reports")
    public ApiResponse<BatchWorkbenchVO> addQualityReport(@PathVariable Long batchId, @Valid @RequestBody QualityReportCreateRequest request) {
        return ApiResponse.ok("质检信息已上传。", batchService.addQualityReport(batchId, request));
    }

    @PostMapping("/{batchId}/risk-actions")
    public ApiResponse<BatchWorkbenchVO> addRiskAction(@PathVariable Long batchId, @Valid @RequestBody BatchRiskActionCreateRequest request) {
        return ApiResponse.ok("风险处理已更新。", batchService.addRiskAction(batchId, request));
    }

    @PostMapping("/{batchId}/qr")
    public ApiResponse<BatchWorkbenchVO> generateQr(@PathVariable Long batchId) {
        return ApiResponse.ok("二维码已准备好。", batchService.generateQr(batchId));
    }
}
