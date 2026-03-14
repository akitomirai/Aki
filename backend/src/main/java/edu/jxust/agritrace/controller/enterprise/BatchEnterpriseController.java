package edu.jxust.agritrace.controller.enterprise;

import com.baomidou.mybatisplus.core.metadata.IPage;
import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.common.log.OperationLogAnnotation;
import edu.jxust.agritrace.module.batch.dto.BatchCreateDTO;
import edu.jxust.agritrace.module.batch.dto.BatchParticipantSaveDTO;
import edu.jxust.agritrace.module.batch.dto.BatchPageQueryDTO;
import edu.jxust.agritrace.module.batch.dto.BatchUpdateDTO;
import edu.jxust.agritrace.module.batch.service.BatchParticipantService;
import edu.jxust.agritrace.module.batch.service.BatchService;
import edu.jxust.agritrace.module.batch.vo.BatchDetailVO;
import edu.jxust.agritrace.module.batch.vo.BatchParticipantVO;
import edu.jxust.agritrace.module.batch.vo.BatchPageItemVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "企业端-批次管理")
@RestController
@RequestMapping("/api/enterprise/batch")
@PreAuthorize("hasAnyRole('ENTERPRISE_ADMIN','ENTERPRISE_USER','ADMIN')")
public class BatchEnterpriseController {

    private final BatchService batchService;
    private final BatchParticipantService batchParticipantService;

    public BatchEnterpriseController(BatchService batchService,
                                     BatchParticipantService batchParticipantService) {
        this.batchService = batchService;
        this.batchParticipantService = batchParticipantService;
    }

    @OperationLogAnnotation(
            module = "BATCH",
            action = "CREATE_BATCH",
            targetType = "TRACE_BATCH"
    )
    @Operation(summary = "创建批次")
    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody BatchCreateDTO dto) {
        return Result.ok(batchService.create(dto));
    }

    @OperationLogAnnotation(
            module = "BATCH",
            action = "UPDATE_BATCH",
            targetType = "TRACE_BATCH"
    )
    @Operation(summary = "修改批次")
    @PutMapping("/update")
    public Result<Void> update(@Valid @RequestBody BatchUpdateDTO dto) {
        batchService.update(dto);
        return Result.ok();
    }

    @Operation(summary = "查询批次详情")
    @GetMapping("/detail/{id}")
    public Result<BatchDetailVO> detail(@PathVariable Long id) {
        return Result.ok(batchService.detail(id));
    }

    @Operation(summary = "分页查询批次")
    @GetMapping("/page")
    public Result<IPage<BatchPageItemVO>> page(BatchPageQueryDTO dto) {
        return Result.ok(batchService.page(dto));
    }

    @OperationLogAnnotation(
            module = "BATCH",
            action = "SAVE_BATCH_PARTICIPANTS",
            targetType = "TRACE_BATCH"
    )
    @Operation(summary = "维护批次参与企业")
    @PostMapping("/participant/save")
    public Result<Void> saveParticipants(@Valid @RequestBody BatchParticipantSaveDTO dto) {
        batchParticipantService.saveForEnterprise(dto);
        return Result.ok();
    }

    @Operation(summary = "查询批次参与企业")
    @GetMapping("/participant/list/{batchId}")
    public Result<List<BatchParticipantVO>> participantList(@PathVariable Long batchId) {
        return Result.ok(batchService.detail(batchId).getParticipants());
    }
}
