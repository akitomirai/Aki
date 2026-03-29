package edu.jxust.agritrace.module.batch.service;

import edu.jxust.agritrace.module.batch.dto.BatchCreateRequest;
import edu.jxust.agritrace.module.batch.dto.BatchListQueryRequest;
import edu.jxust.agritrace.module.batch.dto.BatchRiskActionCreateRequest;
import edu.jxust.agritrace.module.batch.dto.BatchStatusActionRequest;
import edu.jxust.agritrace.module.batch.dto.BatchUpdateRequest;
import edu.jxust.agritrace.module.batch.dto.QualityReportCreateRequest;
import edu.jxust.agritrace.module.batch.dto.TraceRecordCreateRequest;
import edu.jxust.agritrace.module.batch.entity.BatchEntity;
import edu.jxust.agritrace.module.batch.vo.AttachmentCleanupResultVO;
import edu.jxust.agritrace.module.batch.vo.BatchListItemVO;
import edu.jxust.agritrace.module.batch.vo.BatchWorkbenchVO;
import edu.jxust.agritrace.module.batch.vo.CompanyOptionVO;
import edu.jxust.agritrace.module.batch.vo.FileAssetVO;
import edu.jxust.agritrace.module.batch.vo.ProductOptionVO;
import edu.jxust.agritrace.module.publictrace.dto.PublicTraceAccessContext;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BatchService {

    List<BatchListItemVO> listBatches(BatchListQueryRequest request);

    BatchWorkbenchVO getBatchWorkbench(Long batchId);

    List<CompanyOptionVO> listCompanyOptions(String keyword);

    List<ProductOptionVO> listProductOptions(Long companyId, String keyword);

    List<FileAssetVO> uploadAttachments(String businessType, List<MultipartFile> files);

    AttachmentCleanupResultVO cleanupExpiredOrphanAttachments();

    BatchWorkbenchVO createBatch(BatchCreateRequest request);

    BatchWorkbenchVO updateBatch(Long batchId, BatchUpdateRequest request);

    BatchWorkbenchVO changeStatus(Long batchId, BatchStatusActionRequest request);

    BatchWorkbenchVO addTraceRecord(Long batchId, TraceRecordCreateRequest request);

    BatchWorkbenchVO addQualityReport(Long batchId, QualityReportCreateRequest request);

    BatchWorkbenchVO addRiskAction(Long batchId, BatchRiskActionCreateRequest request);

    BatchWorkbenchVO generateQr(Long batchId);

    BatchEntity getBatchEntityById(Long batchId);

    BatchEntity getBatchEntityByToken(String token);

    void recordPublicTraceAccess(String token, PublicTraceAccessContext accessContext);

    Resource loadQrImage(String token);

    Resource loadAttachment(Long fileId);
}
