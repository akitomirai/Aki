package edu.jxust.agritrace.controller.regulator;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.common.log.OperationLogAnnotation;
import edu.jxust.agritrace.module.batch.dto.BatchPageQueryDTO;
import edu.jxust.agritrace.module.batch.service.BatchQueryService;
import edu.jxust.agritrace.module.batch.vo.BatchDetailVO;
import edu.jxust.agritrace.module.batch.vo.BatchPageItemVO;
import edu.jxust.agritrace.module.regulation.dto.BatchStatusActionDTO;
import edu.jxust.agritrace.module.regulation.dto.RegulationCreateDTO;
import edu.jxust.agritrace.module.regulation.dto.RegulationStatusUpdateDTO;
import edu.jxust.agritrace.module.regulation.service.RegulationService;
import edu.jxust.agritrace.module.regulation.vo.RegulationRecordVO;
import jakarta.validation.Valid;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

/**
 * 监管端-批次监管接口
 */
@Tag(name = "监管端-批次监管")
@RestController
@RequestMapping("/api/regulator/batch")
@PreAuthorize("hasRole('REGULATOR')")
public class RegulationController {

    private final RegulationService regulationService;
    private final BatchQueryService batchQueryService;

    public RegulationController(RegulationService regulationService, BatchQueryService batchQueryService) {
        this.regulationService = regulationService;
        this.batchQueryService = batchQueryService;
    }

    @Operation(summary = "分页查询批次")
    @GetMapping("/page")
    public Result<IPage<BatchPageItemVO>> page(BatchPageQueryDTO dto) {
        return Result.ok(batchQueryService.page(dto));
    }

    @Operation(summary = "查询批次详情")
    @GetMapping("/detail/{id}")
    public Result<BatchDetailVO> detail(@PathVariable Long id) {
        return Result.ok(batchQueryService.detail(id));
    }

    @OperationLogAnnotation(
            module = "REGULATION",
            action = "CREATE_REGULATION_RECORD",
            targetType = "REGULATION_RECORD"
    )
    @Operation(summary = "新增监管记录")
    @PostMapping("/regulation-record/create")
    public Result<Long> createRecord(@Valid @RequestBody RegulationCreateDTO dto) {
        return Result.ok(regulationService.createRecord(dto));
    }

    @Operation(summary = "查询监管记录列表")
    @GetMapping("/regulation-record/list/{batchId}")
    public Result<List<RegulationRecordVO>> list(@PathVariable Long batchId) {
        return Result.ok(regulationService.listByBatchId(batchId));
    }

    @OperationLogAnnotation(
            module = "REGULATION",
            action = "UPDATE_REGULATION_STATUS",
            targetType = "TRACE_BATCH"
    )
    @Operation(summary = "更新监管状态")
    @PutMapping("/regulation-status/update")
    public Result<Boolean> updateRegulationStatus(@Valid @RequestBody RegulationStatusUpdateDTO dto) {
        regulationService.updateRegulationStatus(dto);
        return Result.ok(true);
    }

    @OperationLogAnnotation(
            module = "REGULATION",
            action = "FREEZE_BATCH",
            targetType = "TRACE_BATCH"
    )
    @Operation(summary = "冻结批次")
    @PostMapping("/freeze")
    public Result<Boolean> freeze(@Valid @RequestBody BatchStatusActionDTO dto) {
        regulationService.freezeBatch(dto);
        return Result.ok(true);
    }

    @OperationLogAnnotation(
            module = "REGULATION",
            action = "RECALL_BATCH",
            targetType = "TRACE_BATCH"
    )
    @Operation(summary = "召回批次")
    @PostMapping("/recall")
    public Result<Boolean> recall(@Valid @RequestBody BatchStatusActionDTO dto) {
        regulationService.recallBatch(dto);
        return Result.ok(true);
    }
}
