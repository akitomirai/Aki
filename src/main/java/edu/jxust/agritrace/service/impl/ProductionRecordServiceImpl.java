package edu.jxust.agritrace.service.impl;

import edu.jxust.agritrace.dto.ProductionRecordCreateDTO;
import edu.jxust.agritrace.entity.ProductionRecord;
import edu.jxust.agritrace.mapper.ProductionRecordMapper;
import edu.jxust.agritrace.service.ProductionRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductionRecordServiceImpl implements ProductionRecordService {

    @Autowired
    private ProductionRecordMapper recordMapper;

    @Override
    public void create(ProductionRecordCreateDTO dto) {
        ProductionRecord record = new ProductionRecord();
        record.setBatchId(dto.getBatchId());
        record.setOperation(dto.getOperation());
        record.setMaterialUsed(dto.getMaterialUsed());
        record.setOperatorId(dto.getOperatorId());
        record.setRecordTime(dto.getRecordTime());
        record.setRemark(dto.getRemark());
        recordMapper.insert(record);
    }

    @Override
    public List<ProductionRecord> listByBatchId(Long batchId) {
        return recordMapper.selectByBatchId(batchId);
    }
}

