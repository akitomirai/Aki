package edu.jxust.agritrace.module.trace.controller;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.module.trace.dto.EventCreateRequest;
import edu.jxust.agritrace.module.trace.entity.TraceEvent;
import edu.jxust.agritrace.module.trace.mapper.TraceEventMapper;
import edu.jxust.agritrace.common.api.service.TraceEventService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 溯源事件管理（后台）
 * 说明：用于各环节人员录入事件数据。
 */
@RestController
@RequestMapping("/api/admin/event")
public class TraceEventAdminController {

    private final TraceEventService traceEventService;
    private final TraceEventMapper traceEventMapper;

    public TraceEventAdminController(TraceEventService traceEventService,
                                     TraceEventMapper traceEventMapper) {
        this.traceEventService = traceEventService;
        this.traceEventMapper = traceEventMapper;
    }

    /**
     * 新增事件：
     * - ADMIN / OPERATOR 允许
     * - REGULATOR 通常只读（不允许新增）
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Result<TraceEvent> create(@Valid @RequestBody EventCreateRequest req) {
        TraceEvent created = traceEventService.create(req);
        return Result.ok(created);
    }

    /**
     * 根据批次ID查询事件列表（按时间升序）
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN','REGULATOR','OPERATOR')")
    public Result<List<TraceEvent>> list(@RequestParam Long batchId) {
        List<TraceEvent> list = traceEventMapper.selectByBatchIdOrderByTime(batchId);
        return Result.ok(list);
    }
}