package edu.jxust.agritrace.module.log.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.jxust.agritrace.common.exception.BizException;
import edu.jxust.agritrace.module.log.dto.LogPageQueryDTO;
import edu.jxust.agritrace.module.log.entity.OperationLog;
import edu.jxust.agritrace.module.log.mapper.OperationLogMapper;
import edu.jxust.agritrace.module.log.service.OperationLogService;
import org.springframework.stereotype.Service;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper mapper;

    public OperationLogServiceImpl(OperationLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(OperationLog log) {
        mapper.insert(log);
    }

    @Override
    public IPage<OperationLog> page(LogPageQueryDTO dto, Long companyId, Long regulatorOrgId) {
        Page<OperationLog> page = new Page<>(dto.getCurrent(), dto.getSize());
        LambdaQueryWrapper<OperationLog> query = Wrappers.lambdaQuery(OperationLog.class)
                .orderByDesc(OperationLog::getCreatedAt);

        if (dto.getModule() != null && !dto.getModule().isEmpty()) {
            query.like(OperationLog::getModule, dto.getModule());
        }
        if (dto.getOperatorName() != null && !dto.getOperatorName().isEmpty()) {
            query.like(OperationLog::getOperatorName, dto.getOperatorName());
        }
        if (dto.getAction() != null && !dto.getAction().isEmpty()) {
            query.like(OperationLog::getAction, dto.getAction());
        }
        if (dto.getResultStatus() != null && !dto.getResultStatus().isEmpty()) {
            query.eq(OperationLog::getResultStatus, dto.getResultStatus());
        }
        if (dto.getStartTime() != null) {
            query.ge(OperationLog::getCreatedAt, dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            query.le(OperationLog::getCreatedAt, dto.getEndTime());
        }

        // 数据隔离核心逻辑
        if (companyId != null) {
            // 仅查询属于该企业的操作日志（通过子查询关联 sys_user）
            query.apply("operator_id IN (SELECT id FROM sys_user WHERE company_id = {0})", companyId);
        } else if (regulatorOrgId != null) {
            // 仅查询属于该监管机构的操作日志
            query.apply("operator_id IN (SELECT id FROM sys_user WHERE regulator_org_id = {0})", regulatorOrgId);
        }

        return mapper.selectPage(page, query);
    }

    @Override
    public OperationLog detail(Long id, Long companyId, Long regulatorOrgId) {
        if (id == null) {
            throw new BizException("日志ID不能为空");
        }

        LambdaQueryWrapper<OperationLog> query = Wrappers.lambdaQuery(OperationLog.class)
                .eq(OperationLog::getId, id);

        if (companyId != null) {
            query.apply("operator_id IN (SELECT id FROM sys_user WHERE company_id = {0})", companyId);
        } else if (regulatorOrgId != null) {
            query.apply("operator_id IN (SELECT id FROM sys_user WHERE regulator_org_id = {0})", regulatorOrgId);
        }

        OperationLog log = mapper.selectOne(query);
        if (log == null) {
            throw new BizException("日志不存在");
        }
        return log;
    }
}
