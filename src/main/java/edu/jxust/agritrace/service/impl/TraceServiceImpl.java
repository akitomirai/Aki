package edu.jxust.agritrace.service.impl;

import edu.jxust.agritrace.common.BizException;
import edu.jxust.agritrace.entity.Product;
import edu.jxust.agritrace.entity.ProductBatch;
import edu.jxust.agritrace.entity.ProductionRecord;
import edu.jxust.agritrace.mapper.ProductBatchMapper;
import edu.jxust.agritrace.mapper.ProductMapper;
import edu.jxust.agritrace.mapper.ProductionRecordMapper;
import edu.jxust.agritrace.service.TraceService;
import edu.jxust.agritrace.vo.TraceResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TraceServiceImpl implements TraceService {

    @Autowired
    private ProductBatchMapper batchMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductionRecordMapper recordMapper;

    @Override
    public TraceResultVO trace(String batchCode) {

        ProductBatch batch = batchMapper.selectByBatchCode(batchCode);
        if (batch == null) {
            throw new BizException("批次不存在：" + batchCode);
        }

        Product product = productMapper.selectById(batch.getProductId());
        List<ProductionRecord> records =
                recordMapper.selectByBatchId(batch.getId());

        TraceResultVO vo = new TraceResultVO();
        vo.setBatch(batch);
        vo.setProduct(product);
        vo.setRecords(records);
        return vo;
    }
}


