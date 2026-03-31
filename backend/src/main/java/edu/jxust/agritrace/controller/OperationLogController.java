package edu.jxust.agritrace.controller;

import edu.jxust.agritrace.common.api.ApiResponse;
import edu.jxust.agritrace.module.log.dto.OperationLogQueryRequest;
import edu.jxust.agritrace.module.log.service.OperationLogService;
import edu.jxust.agritrace.module.log.vo.OperationLogPageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public ApiResponse<OperationLogPageVO> listLogs(@ModelAttribute OperationLogQueryRequest request) {
        return ApiResponse.ok(operationLogService.listLogs(request));
    }
}
