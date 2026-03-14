package edu.jxust.agritrace.module.batch.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import edu.jxust.agritrace.module.batch.dto.BatchPageQueryDTO;
import edu.jxust.agritrace.module.batch.vo.BatchDetailVO;
import edu.jxust.agritrace.module.batch.vo.BatchPageItemVO;

/**
 * 批次查询服务（平台/监管端最小兼容）
 */
public interface BatchQueryService {

    /**
     * 分页查询批次（不过滤企业）
     */
    IPage<BatchPageItemVO> page(BatchPageQueryDTO dto);

    /**
     * 查询批次详情（不过滤企业）
     */
    BatchDetailVO detail(Long id);
}

