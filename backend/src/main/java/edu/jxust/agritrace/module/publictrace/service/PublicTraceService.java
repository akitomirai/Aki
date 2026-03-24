package edu.jxust.agritrace.module.publictrace.service;

import edu.jxust.agritrace.module.publictrace.vo.PublicTraceDetailVO;

public interface PublicTraceService {

    PublicTraceDetailVO getTraceDetailByToken(String token);
}
