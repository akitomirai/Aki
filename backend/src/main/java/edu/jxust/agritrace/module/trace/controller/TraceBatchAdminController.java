package edu.jxust.agritrace.module.trace.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import edu.jxust.agritrace.common.api.Result;
import edu.jxust.agritrace.module.trace.dto.BatchCreateRequest;
import edu.jxust.agritrace.module.trace.entity.TraceBatch;
import edu.jxust.agritrace.common.api.service.TraceBatchService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


/**
 * 批次管理（后台）：
 * 说明：路径放在 /api/admin 下，必须携带 JWT。
 */
@RestController
@RequestMapping("/api/admin/batch")
public class TraceBatchAdminController {

    private final TraceBatchService traceBatchService;

    public TraceBatchAdminController(TraceBatchService traceBatchService) {
        this.traceBatchService = traceBatchService;
    }

    /**
     * 创建批次：仅管理员可创建（你也可以放宽到 OPERATOR）。
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<TraceBatch> create(@Valid @RequestBody BatchCreateRequest req) {
        TraceBatch created = traceBatchService.create(req);
        return Result.ok(created);
    }

    /**
     * 分页查询批次
     */
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN','REGULATOR')")
    public Result<IPage<TraceBatch>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {

        IPage<TraceBatch> page = traceBatchService.page(current, size);
        return Result.ok(page);
    }

    /**
     * 批次详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','REGULATOR','OPERATOR')")
    public Result<? extends Object> detail(@PathVariable Long id) {
        TraceBatch batch = traceBatchService.getById(id);
        if (batch == null) {
            return Result.fail("批次不存在");
        }
        return Result.ok(batch);
    }
}