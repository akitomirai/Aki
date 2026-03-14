package edu.jxust.agritrace.controller.enterprise;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.common.log.OperationLogAnnotation;
import edu.jxust.agritrace.module.trace.dto.TraceEventCreateDTO;
import edu.jxust.agritrace.module.trace.dto.TraceEventListQueryDTO;
import edu.jxust.agritrace.module.trace.service.TraceEventService;
import edu.jxust.agritrace.module.trace.vo.TraceEventItemVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * 企业端-溯源事件管理接口
 */
@Tag(name = "企业端-溯源事件管理")
@RestController
@RequestMapping({"/api/admin/trace-event", "/api/enterprise/trace-event"})
@PreAuthorize("hasAnyRole('ADMIN','ENTERPRISE_ADMIN','ENTERPRISE_USER')")
public class TraceEventEnterpriseController {

    private final TraceEventService traceEventService;

    public TraceEventEnterpriseController(TraceEventService traceEventService) {
        this.traceEventService = traceEventService;
    }

    /**
     * 新增溯源事件
     */
    @OperationLogAnnotation(
            module = "TRACE_EVENT",
            action = "CREATE_TRACE_EVENT",
            targetType = "TRACE_EVENT"
    )
    @Operation(summary = "新增溯源事件")
    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody TraceEventCreateDTO dto) {
        return Result.ok(traceEventService.create(dto));
    }

    /**
     * 批次事件列表
     */
    @Operation(summary = "查询批次事件列表")
    @GetMapping("/list")
    public Result<List<TraceEventItemVO>> list(TraceEventListQueryDTO dto) {
        return Result.ok(traceEventService.list(dto));
    }

    /**
     * 删除溯源事件
     */
    @OperationLogAnnotation(
            module = "TRACE_EVENT",
            action = "DELETE_TRACE_EVENT",
            targetType = "TRACE_EVENT"
    )
    @Operation(summary = "删除溯源事件")
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> delete(@PathVariable Long id) {
        traceEventService.delete(id);
        return Result.ok(true);
    }
}
