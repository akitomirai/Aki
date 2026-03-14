package edu.jxust.agritrace.module.batch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jxust.agritrace.module.batch.dto.BatchPageQueryDTO;
import edu.jxust.agritrace.module.batch.entity.TraceBatch;
import edu.jxust.agritrace.module.batch.vo.BatchDetailVO;
import edu.jxust.agritrace.module.batch.vo.BatchPageItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 批次 Mapper
 */
@Mapper
public interface TraceBatchMapper extends BaseMapper<TraceBatch> {

    /**
     * 查询批次详情
     */
    BatchDetailVO selectDetailById(@Param("id") Long id);

    /**
     * 分页查询批次
     */
    Page<BatchPageItemVO> selectPageList(Page<BatchPageItemVO> page,
                                         @Param("q") BatchPageQueryDTO queryDTO,
                                         @Param("companyId") Long companyId);
}