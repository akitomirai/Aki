package edu.jxust.agritrace.controller.platform;

import com.baomidou.mybatisplus.core.metadata.IPage;
import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.common.log.OperationLogAnnotation;
import edu.jxust.agritrace.module.batch.dto.BatchParticipantSaveDTO;
import edu.jxust.agritrace.module.batch.dto.BatchPageQueryDTO;
import edu.jxust.agritrace.module.batch.service.BatchParticipantService;
import edu.jxust.agritrace.module.batch.service.BatchQueryService;
import edu.jxust.agritrace.module.batch.vo.BatchDetailVO;
import edu.jxust.agritrace.module.batch.vo.BatchParticipantVO;
import edu.jxust.agritrace.module.batch.vo.BatchPageItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台端-批次查询接口（最小闭环）
 */
@Tag(name = "平台端-批次管理")
@RestController
@RequestMapping("/api/platform/batch")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class BatchPlatformController {

    private final BatchQueryService batchQueryService;
    private final BatchParticipantService batchParticipantService;

    public BatchPlatformController(BatchQueryService batchQueryService,
                                   BatchParticipantService batchParticipantService) {
        this.batchQueryService = batchQueryService;
        this.batchParticipantService = batchParticipantService;
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
            module = "BATCH",
            action = "SAVE_BATCH_PARTICIPANTS",
            targetType = "TRACE_BATCH"
    )
    @Operation(summary = "维护批次参与企业")
    @PostMapping("/participant/save")
    public Result<Void> saveParticipants(@Valid @RequestBody BatchParticipantSaveDTO dto) {
        batchParticipantService.saveForPlatform(dto);
        return Result.ok();
    }

    @Operation(summary = "查询批次参与企业")
    @GetMapping("/participant/list/{batchId}")
    public Result<List<BatchParticipantVO>> participantList(@PathVariable Long batchId) {
        return Result.ok(batchParticipantService.listByBatchId(batchId));
    }
}
