package edu.jxust.agritrace.controller.enterprise;

import com.baomidou.mybatisplus.core.metadata.IPage;
import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.common.security.SecurityUtils;
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
 * 企业端-操作日志接口
 */
@Tag(name = "企业端-操作日志管理")
@RestController
@RequestMapping("/api/enterprise/log")
@PreAuthorize("hasRole('ENTERPRISE_ADMIN') or hasRole('ADMIN')")
public class LogEnterpriseController {

    private final OperationLogService logService;

    public LogEnterpriseController(OperationLogService logService) {
        this.logService = logService;
    }

    /**
     * 分页查询本企业日志
     */
    @Operation(summary = "分页查询本企业日志")
    @GetMapping("/page")
    public Result<IPage<OperationLog>> page(LogPageQueryDTO dto) {
        return Result.ok(logService.page(dto, SecurityUtils.getCompanyId(), null));
    }

    @Operation(summary = "查询日志详情")
    @GetMapping("/detail/{id}")
    public Result<OperationLog> detail(@PathVariable Long id) {
        return Result.ok(logService.detail(id, SecurityUtils.getCompanyId(), null));
    }
}
