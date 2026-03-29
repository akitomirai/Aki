package edu.jxust.agritrace.controller;

import edu.jxust.agritrace.common.api.ApiResponse;
import edu.jxust.agritrace.module.publictrace.dto.PublicTraceAccessContext;
import edu.jxust.agritrace.module.publictrace.service.PublicTraceService;
import edu.jxust.agritrace.module.publictrace.vo.PublicTraceDetailVO;
import jakarta.servlet.http.HttpServletRequest;
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
    public ApiResponse<PublicTraceDetailVO> getTraceDetail(@PathVariable String token, HttpServletRequest request) {
        return ApiResponse.ok(publicTraceService.getTraceDetailByToken(token, buildAccessContext(request)));
    }

    private PublicTraceAccessContext buildAccessContext(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String ip = forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr()
                : forwardedFor.split(",")[0].trim();
        return new PublicTraceAccessContext(
                ip,
                request.getHeader("User-Agent"),
                request.getHeader("Referer")
        );
    }
}
