package edu.jxust.agritrace.mapper;

import edu.jxust.agritrace.entity.ProductionRecord;
import java.util.List;

public interface ProductionRecordMapper {
    void insert(ProductionRecord record);
    List<ProductionRecord> selectByBatchId(Long batchId);
}
