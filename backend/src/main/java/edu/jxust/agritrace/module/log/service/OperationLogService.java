package edu.jxust.agritrace.module.log.service;

import edu.jxust.agritrace.module.log.dto.OperationLogQueryRequest;
import edu.jxust.agritrace.module.log.dto.OperationLogRecord;
import edu.jxust.agritrace.module.log.vo.OperationLogPageVO;

public interface OperationLogService {

    void record(OperationLogRecord record);

    OperationLogPageVO listLogs(OperationLogQueryRequest request);
}
