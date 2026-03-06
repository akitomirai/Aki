package edu.jxust.agritrace.common.api.service.impl;

import edu.jxust.agritrace.common.api.service.TraceBatchService;
import edu.jxust.agritrace.module.trace.dto.BatchCreateRequest;
import edu.jxust.agritrace.module.trace.entity.TraceBatch;
import edu.jxust.agritrace.module.trace.mapper.TraceBatchMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 批次服务实现。
 */
@Service
public
class TraceBatchServiceImpl implements TraceBatchService {

    private final TraceBatchMapper traceBatchMapper;

    public TraceBatchServiceImpl(TraceBatchMapper traceBatchMapper) {
        this.traceBatchMapper = traceBatchMapper;
    }

    /**
     * 创建批次：
     * batchCode 生成规则（简单且够用）：
     *   B + yyyyMMdd + 8位随机
     */
    @Override
    @Transactional
    public TraceBatch create(BatchCreateRequest req) {
        TraceBatch b = new TraceBatch();
        b.setProductId(req.getProductId());
        b.setCompanyId(req.getCompanyId());
        b.setOriginPlace(req.getOriginPlace());
        b.setStartDate(req.getStartDate() == null ? LocalDate.now() : req.getStartDate());
        b.setStatus("ACTIVE");
        b.setRemark(req.getRemark());

        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String rand = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        b.setBatchCode("B" + date + rand);

        traceBatchMapper.insert(b);
        return b;
    }

    /**
     * 分页查询批次
     */
    @Override
    public IPage<TraceBatch> page(long current, long size) {
        Page<TraceBatch> page = new Page<>(current, size);
        return traceBatchMapper.selectPage(page, null);
    }

    /**
     * 根据ID查询批次
     */
    @Override
    public TraceBatch getById(Long id) {
        return traceBatchMapper.selectById(id);
    }
}