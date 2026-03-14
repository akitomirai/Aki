package edu.jxust.agritrace.controller.publicapi;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.module.trace.service.TraceEventService;
import edu.jxust.agritrace.module.trace.vo.PublicTraceTimelineVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前台公开溯源时间轴接口
 */
@RestController
@RequestMapping("/api/public/trace-event")
public class TraceEventPublicController {

    private final TraceEventService traceEventService;

    public TraceEventPublicController(TraceEventService traceEventService) {
        this.traceEventService = traceEventService;
    }

    /**
     * 查询前台时间轴
     */
    @GetMapping("/timeline/{batchId}")
    public Result<List<PublicTraceTimelineVO>> timeline(@PathVariable Long batchId) {
        return Result.ok(traceEventService.publicTimeline(batchId));
    }
}