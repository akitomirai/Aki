package edu.jxust.agritrace.controller.regulator;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.module.feedback.service.ConsumerFeedbackService;
import edu.jxust.agritrace.module.feedback.vo.FeedbackVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 监管端-消费者反馈查看接口（只读最小兼容）
 */
@Tag(name = "监管端-消费者反馈管理")
@RestController
@RequestMapping("/api/regulator/feedback")
@PreAuthorize("hasRole('REGULATOR')")
public class FeedbackRegulatorController {

    private final ConsumerFeedbackService consumerFeedbackService;

    public FeedbackRegulatorController(ConsumerFeedbackService consumerFeedbackService) {
        this.consumerFeedbackService = consumerFeedbackService;
    }

    @Operation(summary = "查询反馈列表")
    @GetMapping("/list")
    public Result<List<FeedbackVO>> list() {
        return Result.ok(consumerFeedbackService.list());
    }

    @Operation(summary = "查询反馈详情")
    @GetMapping("/detail/{id}")
    public Result<FeedbackVO> detail(@PathVariable Long id) {
        return Result.ok(consumerFeedbackService.detail(id));
    }
}

