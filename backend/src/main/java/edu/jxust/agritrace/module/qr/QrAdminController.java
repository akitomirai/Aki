package edu.jxust.agritrace.module.qr;

import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.module.qr.dto.QrGenerateRequest;
import edu.jxust.agritrace.module.qr.service.QrCodeService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 二维码管理（后台）
 */
@RestController
@RequestMapping("/api/admin/qr")
public class QrAdminController {

    private final QrCodeService qrCodeService;

    public QrAdminController(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    /**
     * 批量生成二维码（管理员）
     */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Map<String, Object>>> generate(@Valid @RequestBody QrGenerateRequest req) {
        List<Map<String, Object>> list = qrCodeService.generate(req);
        return Result.ok(list);
    }
}