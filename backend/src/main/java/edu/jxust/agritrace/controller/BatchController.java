package edu.jxust.agritrace.controller;

import edu.jxust.agritrace.common.api.ApiResponse;
import edu.jxust.agritrace.module.batch.dto.BatchCreateRequest;
import edu.jxust.agritrace.module.batch.dto.BatchStatusActionRequest;
import edu.jxust.agritrace.module.batch.dto.QualityReportCreateRequest;
import edu.jxust.agritrace.module.batch.dto.TraceRecordCreateRequest;
import edu.jxust.agritrace.module.batch.service.BatchService;
import edu.jxust.agritrace.module.batch.vo.BatchListItemVO;
import edu.jxust.agritrace.module.batch.vo.BatchWorkbenchVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    private final BatchService batchService;

    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @GetMapping
    public ApiResponse<List<BatchListItemVO>> listBatches() {
        return ApiResponse.ok(batchService.listBatches());
    }

    @GetMapping("/{batchId}")
    public ApiResponse<BatchWorkbenchVO> getBatchWorkbench(@PathVariable Long batchId) {
        return ApiResponse.ok(batchService.getBatchWorkbench(batchId));
    }

    @PostMapping
    public ApiResponse<BatchWorkbenchVO> createBatch(@Valid @RequestBody BatchCreateRequest request) {
        return ApiResponse.ok("批次创建成功", batchService.createBatch(request));
    }

    @PostMapping("/{batchId}/status")
    public ApiResponse<BatchWorkbenchVO> changeStatus(@PathVariable Long batchId, @Valid @RequestBody BatchStatusActionRequest request) {
        return ApiResponse.ok("批次状态已更新", batchService.changeStatus(batchId, request));
    }

    @PostMapping("/{batchId}/records")
    public ApiResponse<BatchWorkbenchVO> addTraceRecord(@PathVariable Long batchId, @Valid @RequestBody TraceRecordCreateRequest request) {
        return ApiResponse.ok("过程记录已补录", batchService.addTraceRecord(batchId, request));
    }

    @PostMapping("/{batchId}/quality-reports")
    public ApiResponse<BatchWorkbenchVO> addQualityReport(@PathVariable Long batchId, @Valid @RequestBody QualityReportCreateRequest request) {
        return ApiResponse.ok("质检报告已补录", batchService.addQualityReport(batchId, request));
    }

    @PostMapping("/{batchId}/qr")
    public ApiResponse<BatchWorkbenchVO> generateQr(@PathVariable Long batchId) {
        return ApiResponse.ok("二维码已生成", batchService.generateQr(batchId));
    }
}
