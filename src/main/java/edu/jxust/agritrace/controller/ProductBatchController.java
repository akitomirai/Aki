package edu.jxust.agritrace.controller;

import edu.jxust.agritrace.common.Result;
import edu.jxust.agritrace.entity.ProductBatch;
import edu.jxust.agritrace.service.ProductBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
public class ProductBatchController {

    @Autowired
    private ProductBatchService batchService;

    @GetMapping
    public Result<List<ProductBatch>> list() {
        return Result.ok(batchService.list());
    }
}
