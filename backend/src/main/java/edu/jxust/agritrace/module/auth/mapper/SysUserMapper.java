package edu.jxust.agritrace.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper（sys_user）。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}