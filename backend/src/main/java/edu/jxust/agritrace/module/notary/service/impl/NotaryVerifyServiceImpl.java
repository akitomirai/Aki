package edu.jxust.agritrace.module.notary.service.impl;

import edu.jxust.agritrace.common.exception.BizException;
import edu.jxust.agritrace.module.batch.entity.TraceBatch;
import edu.jxust.agritrace.module.batch.mapper.TraceBatchMapper;
import edu.jxust.agritrace.module.notary.constant.NotaryBizType;
import edu.jxust.agritrace.module.notary.entity.HashNotary;
import edu.jxust.agritrace.module.notary.mapper.HashNotaryMapper;
import edu.jxust.agritrace.module.notary.service.HashNotaryService;
import edu.jxust.agritrace.module.notary.service.NotaryVerifyService;
import edu.jxust.agritrace.common.util.NotarySnapshotBuilder;
import edu.jxust.agritrace.module.notary.vo.NotaryVerifyVO;
import edu.jxust.agritrace.module.quality.entity.QualityReport;
import edu.jxust.agritrace.module.quality.mapper.QualityReportMapper;
import edu.jxust.agritrace.module.regulation.entity.RegulationRecord;
import edu.jxust.agritrace.module.regulation.mapper.RegulationRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class NotaryVerifyServiceImpl implements NotaryVerifyService {

    private final HashNotaryMapper hashNotaryMapper;
    private final HashNotaryService hashNotaryService;
    private final TraceBatchMapper traceBatchMapper;
    private final RegulationRecordMapper regulationRecordMapper;
    private final QualityReportMapper qualityReportMapper;

    public NotaryVerifyServiceImpl(HashNotaryMapper hashNotaryMapper,
                                   HashNotaryService hashNotaryService,
                                   TraceBatchMapper traceBatchMapper,
                                   RegulationRecordMapper regulationRecordMapper,
                                   QualityReportMapper qualityReportMapper) {
        this.hashNotaryMapper = hashNotaryMapper;
        this.hashNotaryService = hashNotaryService;
        this.traceBatchMapper = traceBatchMapper;
        this.regulationRecordMapper = regulationRecordMapper;
        this.qualityReportMapper = qualityReportMapper;
    }

    @Override
    public NotaryVerifyVO verifyBatch(Long batchId) {
        TraceBatch batch = traceBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BizException("批次不存在");
        }

        HashNotary hashNotary = hashNotaryMapper.selectByBiz(NotaryBizType.TRACE_BATCH, batchId);
        if (hashNotary == null) {
            throw new BizException("该批次尚未存证");
        }

        String snapshot = NotarySnapshotBuilder.buildBatchSnapshot(batch);
        String currentHash = hashNotaryService.sha256(snapshot);

        return buildResult(
                NotaryBizType.TRACE_BATCH,
                batchId,
                hashNotary.getSha256(),
                currentHash
        );
    }

    @Override
    public NotaryVerifyVO verifyRegulationRecord(Long recordId) {
        RegulationRecord record = regulationRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BizException("监管记录不存在");
        }

        HashNotary hashNotary = hashNotaryMapper.selectByBiz(NotaryBizType.REGULATION_RECORD, recordId);
        if (hashNotary == null) {
            throw new BizException("该监管记录尚未存证");
        }

        String snapshot = NotarySnapshotBuilder.buildRegulationSnapshot(record);
        String currentHash = hashNotaryService.sha256(snapshot);

        return buildResult(
                NotaryBizType.REGULATION_RECORD,
                recordId,
                hashNotary.getSha256(),
                currentHash
        );
    }

    @Override
    public NotaryVerifyVO verifyQualityReport(Long reportId) {
        QualityReport report = qualityReportMapper.selectById(reportId);
        if (report == null) {
            throw new BizException("质检报告不存在");
        }

        HashNotary hashNotary = hashNotaryMapper.selectByBiz(NotaryBizType.QUALITY_REPORT, reportId);
        if (hashNotary == null) {
            throw new BizException("该质检报告尚未存证");
        }

        String snapshot = NotarySnapshotBuilder.buildQualitySnapshot(report);
        String currentHash = hashNotaryService.sha256(snapshot);

        return buildResult(
                NotaryBizType.QUALITY_REPORT,
                reportId,
                hashNotary.getSha256(),
                currentHash
        );
    }

    private NotaryVerifyVO buildResult(String bizType, Long bizId, String storedHash, String currentHash) {
        NotaryVerifyVO vo = new NotaryVerifyVO();
        vo.setBizType(bizType);
        vo.setBizId(bizId);
        vo.setStoredHash(storedHash);
        vo.setCurrentHash(currentHash);

        boolean matched = storedHash != null && storedHash.equals(currentHash);
        vo.setMatched(matched);
        vo.setMessage(matched ? "校验通过，当前数据与存证摘要一致" : "校验失败，当前数据与存证摘要不一致");

        return vo;
    }
}