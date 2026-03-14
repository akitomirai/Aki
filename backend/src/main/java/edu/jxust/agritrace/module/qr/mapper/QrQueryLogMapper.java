package edu.jxust.agritrace.module.qr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.qr.entity.QrQueryLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 扫码日志 Mapper
 */
@Mapper
public interface QrQueryLogMapper extends BaseMapper<QrQueryLog> {
}