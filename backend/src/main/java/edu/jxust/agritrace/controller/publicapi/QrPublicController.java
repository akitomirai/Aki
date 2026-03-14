package edu.jxust.agritrace.controller.publicapi;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.module.qr.service.QrService;
import edu.jxust.agritrace.module.qr.vo.PublicQrScanVO;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "公共接口-扫码溯源")
@RestController
@RequestMapping("/api/public/qr")
public class QrPublicController {

    private final QrService qrService;

    public QrPublicController(QrService qrService) {
        this.qrService = qrService;
    }

    @Operation(summary = "扫码查询溯源信息")
    @GetMapping("/scan/{token}")
    public Result<PublicQrScanVO> scan(@PathVariable String token) {
        return Result.ok(qrService.scan(token));
    }
}