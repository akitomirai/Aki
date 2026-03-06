package edu.jxust.agritrace.module.quality.service;

import edu.jxust.agritrace.module.quality.dto.QualityReportCreateRequest;
import edu.jxust.agritrace.module.quality.entity.QualityReport;

/**
 * 质检报告服务
 */
public interface QualityReportService {

    /**
     * 新增质检报告，并生成哈希存证
     */
    QualityReport createWithNotary(QualityReportCreateRequest req);

    /**
     * 校验最新质检报告是否与存证一致
     */
    boolean verifyLatest(long batchId);

    boolean reNotary(long reportId);

}