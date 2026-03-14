package edu.jxust.agritrace.controller.platform;

import com.baomidou.mybatisplus.core.metadata.IPage;
import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.module.log.dto.LogPageQueryDTO;
import edu.jxust.agritrace.module.log.entity.OperationLog;
import edu.jxust.agritrace.module.log.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台端-操作日志接口
 */
@Tag(name = "平台端-操作日志管理")
@RestController
@RequestMapping("/api/platform/log")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class LogPlatformController {

    private final OperationLogService logService;

    public LogPlatformController(OperationLogService logService) {
        this.logService = logService;
    }

    /**
     * 分页查询全局日志
     */
    @Operation(summary = "分页查询全局日志")
    @GetMapping("/page")
    public Result<IPage<OperationLog>> page(LogPageQueryDTO dto) {
        return Result.ok(logService.page(dto, null, null));
    }

    @Operation(summary = "查询日志详情")
    @GetMapping("/detail/{id}")
    public Result<OperationLog> detail(@PathVariable Long id) {
        return Result.ok(logService.detail(id, null, null));
    }
}
