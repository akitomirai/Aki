package edu.jxust.agritrace.common.api.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import edu.jxust.agritrace.module.trace.dto.BatchCreateRequest;
import edu.jxust.agritrace.module.trace.entity.TraceBatch;

/**
 * 批次服务：
 * - 创建批次
 * - 查询批次
 */
public interface TraceBatchService {

    /**
     * 创建批次并返回创建后的实体。
     */
    TraceBatch create(BatchCreateRequest req);

    /**
     * 分页查询批次（按 id 倒序）
     */
    IPage<TraceBatch> page(long current, long size);

    /**
     * 根据ID查询批次
     */
    TraceBatch getById(Long id);
}