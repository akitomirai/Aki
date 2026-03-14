package edu.jxust.agritrace.controller.enterprise;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.common.log.OperationLogAnnotation;
import edu.jxust.agritrace.common.security.SecurityUtils;
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
 * 企业端-消费者反馈管理接口
 */
@Tag(name = "企业端-消费者反馈管理")
@RestController
@RequestMapping("/api/enterprise/feedback")
@PreAuthorize("hasAnyRole('ENTERPRISE_ADMIN','ENTERPRISE_USER','ADMIN')")
public class FeedbackEnterpriseController {

    private final ConsumerFeedbackService consumerFeedbackService;

    public FeedbackEnterpriseController(ConsumerFeedbackService consumerFeedbackService) {
        this.consumerFeedbackService = consumerFeedbackService;
    }

    /**
     * 查看本企业反馈列表
     */
    @Operation(summary = "查看本企业反馈列表")
    @GetMapping("/list")
    public Result<List<FeedbackVO>> list() {
        Long companyId = SecurityUtils.getCompanyId();
        return Result.ok(consumerFeedbackService.selectEnterpriseList(companyId));
    }

    /**
     * 查看反馈详情
     */
    @Operation(summary = "查看反馈详情")
    @GetMapping("/detail/{id}")
    public Result<FeedbackVO> detail(@PathVariable Long id) {
        Long companyId = SecurityUtils.getCompanyId();
        return Result.ok(consumerFeedbackService.selectEnterpriseDetail(id, companyId));
    }

    /**
     * 回复反馈
     */
    @OperationLogAnnotation(
            module = "FEEDBACK",
            action = "HANDLE_FEEDBACK",
            targetType = "CONSUMER_FEEDBACK"
    )
    @Operation(summary = "回复反馈")
    @PutMapping("/handle")
    public Result<Boolean> handle(@Valid @RequestBody FeedbackHandleDTO dto) {
        Long companyId = SecurityUtils.getCompanyId();
        consumerFeedbackService.handleByCompanyId(dto, companyId);
        return Result.ok(true);
    }
}
