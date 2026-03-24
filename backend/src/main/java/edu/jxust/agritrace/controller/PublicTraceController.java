package edu.jxust.agritrace.controller;

import edu.jxust.agritrace.common.api.ApiResponse;
import edu.jxust.agritrace.module.publictrace.service.PublicTraceService;
import edu.jxust.agritrace.module.publictrace.vo.PublicTraceDetailVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/traces")
public class PublicTraceController {

    private final PublicTraceService publicTraceService;

    public PublicTraceController(PublicTraceService publicTraceService) {
        this.publicTraceService = publicTraceService;
    }

    @GetMapping("/{token}")
    public ApiResponse<PublicTraceDetailVO> getTraceDetail(@PathVariable String token) {
        return ApiResponse.ok(publicTraceService.getTraceDetailByToken(token));
    }
}
