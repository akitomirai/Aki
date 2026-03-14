package edu.jxust.agritrace.module.quality.service;

import edu.jxust.agritrace.module.quality.dto.QualityReportCreateDTO;
import edu.jxust.agritrace.module.quality.dto.QualityReportUpdateDTO;
import edu.jxust.agritrace.module.quality.vo.QualityReportVO;
import edu.jxust.agritrace.module.quality.vo.QualityVerifyVO;

import java.util.List;

public interface QualityReportService {

    Long create(QualityReportCreateDTO dto);

    void update(QualityReportUpdateDTO dto);

    List<QualityReportVO> listByBatchId(Long batchId);

    QualityReportVO detail(Long id);

    QualityVerifyVO verifyLatestByBatchId(Long batchId);

    void delete(Long id);
}
