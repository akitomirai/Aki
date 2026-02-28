package edu.jxust.agritrace.controller;

import edu.jxust.agritrace.common.Result;
import edu.jxust.agritrace.service.TraceService;
import edu.jxust.agritrace.vo.TraceResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trace")
public class TraceController {

    @Autowired
    private TraceService traceService;

    @GetMapping("/{batchCode}")
    public Result<TraceResultVO> trace(@PathVariable String batchCode) {
        return Result.ok(traceService.trace(batchCode));
    }
}


