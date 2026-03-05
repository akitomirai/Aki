package edu.jxust.agritrace.module.publicapi;

import edu.jxust.agritrace.module.trace.mapper.TraceBatchMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 公共健康检查接口：
 * - /api/public/health：验证服务启动
 * - /api/public/db：验证数据库可用
 */
@RestController
public class HealthController {

    private final TraceBatchMapper traceBatchMapper;

    public HealthController(TraceBatchMapper traceBatchMapper) {
        this.traceBatchMapper = traceBatchMapper;
    }

    /**
     * 服务健康检查：返回 ok=true
     */
    @GetMapping("/api/public/health")
    public Map<String, Object> health() {
        return Map.of("ok", true);
    }

    /**
     * 数据库检查：查询 trace_batch 表记录数（0 也正常）
     */
    @GetMapping("/api/public/db")
    public Map<String, Object> db() {
        Long count = traceBatchMapper.selectCount(null);
        return Map.of("ok", true, "trace_batch_count", count);
    }
}