package edu.jxust.agritrace.module.regulation.service;

import edu.jxust.agritrace.module.regulation.dto.BatchStatusActionDTO;
import edu.jxust.agritrace.module.regulation.dto.RegulationCreateDTO;
import edu.jxust.agritrace.module.regulation.dto.RegulationStatusUpdateDTO;
import edu.jxust.agritrace.module.regulation.vo.RegulationRecordVO;

import java.util.List;

/**
 * 监管服务接口
 */
public interface RegulationService {

    /**
     * 新增监管记录
     *
     * @param dto 新增参数
     * @return 记录ID
     */
    Long createRecord(RegulationCreateDTO dto);

    /**
     * 查询监管记录列表
     *
     * @param batchId 批次ID
     * @return 监管记录列表
     */
    List<RegulationRecordVO> listByBatchId(Long batchId);

    /**
     * 查询公开监管记录列表
     *
     * @param batchId 批次ID
     * @return 监管记录列表
     */
    List<RegulationRecordVO> listPublicByBatchId(Long batchId);

    /**
     * 更新监管状态
     *
     * @param dto 更新参数
     */
    void updateRegulationStatus(RegulationStatusUpdateDTO dto);

    /**
     * 冻结批次
     *
     * @param dto 参数
     */
    void freezeBatch(BatchStatusActionDTO dto);

    /**
     * 召回批次
     *
     * @param dto 参数
     */
    void recallBatch(BatchStatusActionDTO dto);
}