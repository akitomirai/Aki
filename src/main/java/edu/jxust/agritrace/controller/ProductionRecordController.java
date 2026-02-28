package edu.jxust.agritrace.controller;

import edu.jxust.agritrace.common.Result;
import edu.jxust.agritrace.dto.ProductionRecordCreateDTO;
import edu.jxust.agritrace.entity.ProductionRecord;
import edu.jxust.agritrace.service.ProductionRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/production-records")
public class ProductionRecordController {

    @Autowired
    private ProductionRecordService recordService;

    @PostMapping
    public Result<Void> create(@RequestBody ProductionRecordCreateDTO dto) {
        recordService.create(dto);
        return Result.ok(null);
    }

    @GetMapping("/by-batch/{batchId}")
    public Result<List<ProductionRecord>> listByBatch(@PathVariable Long batchId) {
        return Result.ok(recordService.listByBatchId(batchId));
    }
}