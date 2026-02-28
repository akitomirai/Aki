package edu.jxust.agritrace.service;

import edu.jxust.agritrace.dto.ProductionRecordCreateDTO;
import edu.jxust.agritrace.entity.ProductionRecord;
import java.util.List;

public interface ProductionRecordService {
    void create(ProductionRecordCreateDTO dto);
    List<ProductionRecord> listByBatchId(Long batchId);
}
