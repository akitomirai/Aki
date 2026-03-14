package edu.jxust.agritrace.controller.publicapi;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.module.trace.service.TracePublicService;
import edu.jxust.agritrace.module.trace.vo.PublicTraceDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前台公开溯源接口（聚合详情）
 */
@Tag(name = "公共接口-聚合溯源详情")
@RestController
@RequestMapping("/api/public/trace")
public class TracePublicController {

    private final TracePublicService tracePublicService;

    public TracePublicController(TracePublicService tracePublicService) {
        this.tracePublicService = tracePublicService;
    }

    /**
     * 获取聚合溯源详情
     * 适配 trace-web 的 getTraceDetail 调用
     */
    @Operation(summary = "获取聚合溯源详情")
    @GetMapping("/detail/{token}")
    public Result<PublicTraceDetailVO> detail(@PathVariable String token) {
        return Result.ok(tracePublicService.getTraceDetail(token));
    }
}
