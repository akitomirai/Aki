package edu.jxust.agritrace.module.batch.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jxust.agritrace.common.exception.BizException;
import edu.jxust.agritrace.module.batch.dto.BatchPageQueryDTO;
import edu.jxust.agritrace.module.batch.mapper.TraceBatchMapper;
import edu.jxust.agritrace.module.batch.service.BatchParticipantService;
import edu.jxust.agritrace.module.batch.service.BatchQueryService;
import edu.jxust.agritrace.module.batch.vo.BatchDetailVO;
import edu.jxust.agritrace.module.batch.vo.BatchPageItemVO;
import edu.jxust.agritrace.module.trace.service.TraceEventService;
import org.springframework.stereotype.Service;

/**
 * 批次查询服务实现（平台/监管端最小兼容）
 */
@Service
public class BatchQueryServiceImpl implements BatchQueryService {

    private final TraceBatchMapper traceBatchMapper;
    private final BatchParticipantService batchParticipantService;
    private final TraceEventService traceEventService;

    public BatchQueryServiceImpl(TraceBatchMapper traceBatchMapper,
                                 BatchParticipantService batchParticipantService,
                                 TraceEventService traceEventService) {
        this.traceBatchMapper = traceBatchMapper;
        this.batchParticipantService = batchParticipantService;
        this.traceEventService = traceEventService;
    }

    @Override
    public IPage<BatchPageItemVO> page(BatchPageQueryDTO dto) {
        Page<BatchPageItemVO> page = new Page<>(dto.getCurrent(), dto.getSize());
        return traceBatchMapper.selectPageList(page, dto, null);
    }

    @Override
    public BatchDetailVO detail(Long id) {
        if (id == null) {
            throw new BizException("批次ID不能为空");
        }
        BatchDetailVO detail = traceBatchMapper.selectDetailById(id);
        if (detail == null) {
            throw new BizException("批次不存在");
        }
        detail.setParticipants(batchParticipantService.listByBatchId(detail.getId()));
        detail.setNodes(traceEventService.nodeList(detail.getId()));
        return detail;
    }
}
