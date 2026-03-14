package edu.jxust.agritrace.controller.publicapi;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.common.log.OperationLogAnnotation;
import edu.jxust.agritrace.module.feedback.dto.FeedbackCreateDTO;
import edu.jxust.agritrace.module.feedback.service.ConsumerFeedbackService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 前台消费者反馈接口
 */
@RestController
@RequestMapping("/api/public/feedback")
public class FeedbackPublicController {

    private final ConsumerFeedbackService consumerFeedbackService;

    public FeedbackPublicController(ConsumerFeedbackService consumerFeedbackService) {
        this.consumerFeedbackService = consumerFeedbackService;
    }

    /**
     * 提交反馈
     */
    @OperationLogAnnotation(module = "FEEDBACK", action = "SUBMIT_FEEDBACK", targetType = "CONSUMER_FEEDBACK")
    @PostMapping("/create")
    public Result<Long> create(@Valid @RequestBody FeedbackCreateDTO dto) {
        return Result.ok(consumerFeedbackService.create(dto));
    }
}