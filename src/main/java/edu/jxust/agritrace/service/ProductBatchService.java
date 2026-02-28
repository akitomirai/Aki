package edu.jxust.agritrace.service;

import edu.jxust.agritrace.entity.ProductBatch;

import java.util.List;

public interface ProductBatchService {
    List<ProductBatch> list();
    ProductBatch getByBatchCode(String batchCode);
}
