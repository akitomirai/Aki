package edu.jxust.agritrace.controller.platform;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.common.log.OperationLogAnnotation;
import edu.jxust.agritrace.module.feedback.dto.FeedbackHandleDTO;
import edu.jxust.agritrace.module.feedback.service.ConsumerFeedbackService;
import edu.jxust.agritrace.module.feedback.vo.FeedbackVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * 平台端-消费者反馈管理接口
 */
@Tag(name = "平台端-消费者反馈管理")
@RestController
@RequestMapping("/api/admin/feedback")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class FeedbackPlatformController {

    private final ConsumerFeedbackService consumerFeedbackService;

    public FeedbackPlatformController(ConsumerFeedbackService consumerFeedbackService) {
        this.consumerFeedbackService = consumerFeedbackService;
    }

    /**
     * 反馈列表
     */
    @Operation(summary = "查询所有反馈列表")
    @GetMapping("/list")
    public Result<List<FeedbackVO>> list() {
        return Result.ok(consumerFeedbackService.list());
    }

    /**
     * 反馈详情
     */
    @Operation(summary = "查询反馈详情")
    @GetMapping("/detail/{id}")
    public Result<FeedbackVO> detail(@PathVariable Long id) {
        return Result.ok(consumerFeedbackService.detail(id));
    }

    /**
     * 处理反馈
     */
    @OperationLogAnnotation(
            module = "FEEDBACK",
            action = "HANDLE_FEEDBACK",
            targetType = "CONSUMER_FEEDBACK"
    )
    @Operation(summary = "处理反馈")
    @PutMapping("/handle")
    public Result<Boolean> handle(@Valid @RequestBody FeedbackHandleDTO dto) {
        consumerFeedbackService.handle(dto);
        return Result.ok(true);
    }

    /**
     * 公开反馈
     */
    @OperationLogAnnotation(
            module = "FEEDBACK",
            action = "PUBLISH_FEEDBACK",
            targetType = "CONSUMER_FEEDBACK"
    )
    @Operation(summary = "公开反馈")
    @PutMapping("/publish/{id}")
    public Result<Boolean> publish(@PathVariable Long id) {
        consumerFeedbackService.publish(id);
        return Result.ok(true);
    }
}
