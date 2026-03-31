package edu.jxust.agritrace.module.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.log.mapper.po.OperationAuditLogPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationAuditLogMapper extends BaseMapper<OperationAuditLogPO> {
}
