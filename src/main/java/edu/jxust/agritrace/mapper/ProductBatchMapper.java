package edu.jxust.agritrace.mapper;

import edu.jxust.agritrace.entity.ProductBatch;
import java.util.List;

public interface ProductBatchMapper {
    List<ProductBatch> selectAll();
    ProductBatch selectByBatchCode(String batchCode);
}
