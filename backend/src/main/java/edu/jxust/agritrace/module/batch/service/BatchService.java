package edu.jxust.agritrace.module.batch.service;

import edu.jxust.agritrace.module.batch.dto.BatchCreateRequest;
import edu.jxust.agritrace.module.batch.dto.BatchStatusActionRequest;
import edu.jxust.agritrace.module.batch.dto.QualityReportCreateRequest;
import edu.jxust.agritrace.module.batch.dto.TraceRecordCreateRequest;
import edu.jxust.agritrace.module.batch.entity.BatchEntity;
import edu.jxust.agritrace.module.batch.vo.BatchListItemVO;
import edu.jxust.agritrace.module.batch.vo.BatchWorkbenchVO;

import java.util.List;

public interface BatchService {

    List<BatchListItemVO> listBatches();

    BatchWorkbenchVO getBatchWorkbench(Long batchId);

    BatchWorkbenchVO createBatch(BatchCreateRequest request);

    BatchWorkbenchVO changeStatus(Long batchId, BatchStatusActionRequest request);

    BatchWorkbenchVO addTraceRecord(Long batchId, TraceRecordCreateRequest request);

    BatchWorkbenchVO addQualityReport(Long batchId, QualityReportCreateRequest request);

    BatchWorkbenchVO generateQr(Long batchId);

    BatchEntity getBatchEntityById(Long batchId);

    BatchEntity getBatchEntityByToken(String token);
}
