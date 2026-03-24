package edu.jxust.agritrace.module.publictrace.service;

import edu.jxust.agritrace.module.publictrace.dto.PublicTraceAccessContext;
import edu.jxust.agritrace.module.publictrace.vo.PublicTraceDetailVO;

public interface PublicTraceService {

    default PublicTraceDetailVO getTraceDetailByToken(String token) {
        return getTraceDetailByToken(token, PublicTraceAccessContext.anonymous());
    }

    PublicTraceDetailVO getTraceDetailByToken(String token, PublicTraceAccessContext accessContext);
}
