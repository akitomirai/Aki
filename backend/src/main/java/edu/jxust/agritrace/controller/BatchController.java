package edu.jxust.agritrace.controller;

import edu.jxust.agritrace.common.api.ApiResponse;
import edu.jxust.agritrace.module.batch.dto.BatchCreateRequest;
import edu.jxust.agritrace.module.batch.dto.BatchListQueryRequest;
import edu.jxust.agritrace.module.batch.dto.BatchStatusActionRequest;
import edu.jxust.agritrace.module.batch.dto.BatchUpdateRequest;
import edu.jxust.agritrace.module.batch.dto.QualityReportCreateRequest;
import edu.jxust.agritrace.module.batch.dto.TraceRecordCreateRequest;
import edu.jxust.agritrace.module.batch.service.BatchService;
import edu.jxust.agritrace.module.batch.vo.BatchListItemVO;
import edu.jxust.agritrace.module.batch.vo.BatchWorkbenchVO;
import edu.jxust.agritrace.module.batch.vo.CompanyOptionVO;
import edu.jxust.agritrace.module.batch.vo.FileAssetVO;
import edu.jxust.agritrace.module.batch.vo.ProductOptionVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
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

    @PostMapping("/files/upload")
    public ApiResponse<List<FileAssetVO>> uploadFiles(
            @RequestParam String businessType,
            @RequestPart("files") List<MultipartFile> files
    ) {
        return ApiResponse.ok(batchService.uploadAttachments(businessType, files));
    }

    @GetMapping("/{batchId}")
    public ApiResponse<BatchWorkbenchVO> getBatchWorkbench(@PathVariable Long batchId) {
        return ApiResponse.ok(batchService.getBatchWorkbench(batchId));
    }

    @PostMapping
    public ApiResponse<BatchWorkbenchVO> createBatch(@Valid @RequestBody BatchCreateRequest request) {
        return ApiResponse.ok("批次创建成功", batchService.createBatch(request));
    }

    @PatchMapping("/{batchId}")
    public ApiResponse<BatchWorkbenchVO> updateBatch(@PathVariable Long batchId, @Valid @RequestBody BatchUpdateRequest request) {
        return ApiResponse.ok("批次基础信息已更新", batchService.updateBatch(batchId, request));
    }

    @PostMapping("/{batchId}/status")
    public ApiResponse<BatchWorkbenchVO> changeStatus(@PathVariable Long batchId, @Valid @RequestBody BatchStatusActionRequest request) {
        return ApiResponse.ok("批次状态已更新", batchService.changeStatus(batchId, request));
    }

    @PostMapping("/{batchId}/records")
    public ApiResponse<BatchWorkbenchVO> addTraceRecord(@PathVariable Long batchId, @Valid @RequestBody TraceRecordCreateRequest request) {
        return ApiResponse.ok("追溯记录已补录", batchService.addTraceRecord(batchId, request));
    }

    @PostMapping("/{batchId}/records/quick")
    public ApiResponse<BatchWorkbenchVO> addQuickTraceRecord(@PathVariable Long batchId, @Valid @RequestBody TraceRecordCreateRequest request) {
        return ApiResponse.ok("追溯记录已快速补录", batchService.addTraceRecord(batchId, request));
    }

    @PostMapping("/{batchId}/quality-reports")
    public ApiResponse<BatchWorkbenchVO> addQualityReport(@PathVariable Long batchId, @Valid @RequestBody QualityReportCreateRequest request) {
        return ApiResponse.ok("质检摘要已更新", batchService.addQualityReport(batchId, request));
    }

    @PostMapping("/{batchId}/qr")
    public ApiResponse<BatchWorkbenchVO> generateQr(@PathVariable Long batchId) {
        return ApiResponse.ok("二维码摘要已准备就绪", batchService.generateQr(batchId));
    }
}
