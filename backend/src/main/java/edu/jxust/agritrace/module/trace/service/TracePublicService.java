package edu.jxust.agritrace.module.trace.service;

import edu.jxust.agritrace.module.trace.vo.PublicTraceDetailVO;

/**
 * 前台公开溯源服务
 */
public interface TracePublicService {

    /**
     * 获取全量聚合溯源详情
     * @param token 二维码 token
     * @return 聚合详情
     */
    PublicTraceDetailVO getTraceDetail(String token);
}
