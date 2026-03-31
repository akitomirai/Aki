package edu.jxust.agritrace.module.log.vo;

import java.util.List;

public record OperationLogPageVO(
        List<OperationLogVO> items,
        long total,
        int page,
        int pageSize
) {
}
