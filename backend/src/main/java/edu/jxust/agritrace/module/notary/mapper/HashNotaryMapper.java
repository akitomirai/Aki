package edu.jxust.agritrace.module.notary.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.notary.entity.HashNotary;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HashNotaryMapper extends BaseMapper<HashNotary> {

    default HashNotary selectByBiz(String bizType, Long bizId) {
        return selectOne(new LambdaQueryWrapper<HashNotary>()
                .eq(HashNotary::getBizType, bizType)
                .eq(HashNotary::getBizId, bizId));
    }
}