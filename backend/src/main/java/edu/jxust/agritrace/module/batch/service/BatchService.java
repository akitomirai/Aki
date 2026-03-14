package edu.jxust.agritrace.module.batch.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import edu.jxust.agritrace.module.batch.dto.BatchCreateDTO;
import edu.jxust.agritrace.module.batch.dto.BatchPageQueryDTO;
import edu.jxust.agritrace.module.batch.dto.BatchUpdateDTO;
import edu.jxust.agritrace.module.batch.vo.BatchDetailVO;
import edu.jxust.agritrace.module.batch.vo.BatchPageItemVO;

/**
 * 批次服务接口
 */
public interface BatchService {

    /**
     * 新增批次
     *
     * @param dto 新增参数
     * @return 新增后的批次ID
     */
    Long create(BatchCreateDTO dto);

    /**
     * 修改批次
     *
     * @param dto 修改参数
     */
    void update(BatchUpdateDTO dto);

    /**
     * 查询批次详情
     *
     * @param id 批次ID
     * @return 批次详情
     */
    BatchDetailVO detail(Long id);

    /**
     * 分页查询批次
     *
     * @param dto 查询参数
     * @return 分页结果
     */
    IPage<BatchPageItemVO> page(BatchPageQueryDTO dto);
}