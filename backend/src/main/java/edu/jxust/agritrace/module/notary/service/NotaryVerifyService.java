package edu.jxust.agritrace.module.notary.service;

import edu.jxust.agritrace.module.notary.vo.NotaryVerifyVO;

public interface NotaryVerifyService {

    NotaryVerifyVO verifyBatch(Long batchId);

    NotaryVerifyVO verifyRegulationRecord(Long recordId);

    NotaryVerifyVO verifyQualityReport(Long reportId);
}