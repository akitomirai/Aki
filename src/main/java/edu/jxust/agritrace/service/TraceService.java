package edu.jxust.agritrace.service;

import edu.jxust.agritrace.vo.TraceResultVO;

public interface TraceService {
    TraceResultVO trace(String batchCode);
}
