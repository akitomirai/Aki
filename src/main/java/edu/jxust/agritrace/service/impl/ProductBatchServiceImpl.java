package edu.jxust.agritrace.service.impl;

import edu.jxust.agritrace.entity.ProductBatch;
import edu.jxust.agritrace.mapper.ProductBatchMapper;
import edu.jxust.agritrace.service.ProductBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductBatchServiceImpl implements ProductBatchService {

    @Autowired
    private ProductBatchMapper batchMapper;

    @Override
    public List<ProductBatch> list() {
        return batchMapper.selectAll();
    }

    @Override
    public ProductBatch getByBatchCode(String batchCode) {
        return batchMapper.selectByBatchCode(batchCode);
    }
}

