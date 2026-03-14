package edu.jxust.agritrace.module.log.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import edu.jxust.agritrace.module.log.dto.LogPageQueryDTO;
import edu.jxust.agritrace.module.log.entity.OperationLog;

public interface OperationLogService {

    void save(OperationLog log);

    /**
     * 分页查询操作日志
     *
     * @param dto            查询条件
     * @param companyId      所属企业ID（若为null则不限，用于平台管理员）
     * @param regulatorOrgId 所属监管机构ID（若为null则不限）
     * @return 分页结果
     */
    IPage<OperationLog> page(LogPageQueryDTO dto, Long companyId, Long regulatorOrgId);

    /**
     * 查询日志详情
     *
     * @param id             日志ID
     * @param companyId      所属企业ID（若为null则不限，用于平台管理员）
     * @param regulatorOrgId 所属监管机构ID（若为null则不限）
     * @return 日志详情
     */
    OperationLog detail(Long id, Long companyId, Long regulatorOrgId);

}
