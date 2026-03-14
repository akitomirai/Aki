package edu.jxust.agritrace.module.batch.service;

import edu.jxust.agritrace.module.batch.dto.BatchParticipantSaveDTO;
import edu.jxust.agritrace.module.batch.vo.BatchParticipantVO;

import java.util.List;

public interface BatchParticipantService {

    void saveForEnterprise(BatchParticipantSaveDTO dto);

    void saveForPlatform(BatchParticipantSaveDTO dto);

    List<BatchParticipantVO> listByBatchId(Long batchId);
}
