package edu.jxust.agritrace.module.feedback.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import edu.jxust.agritrace.module.feedback.entity.ConsumerFeedback;
import edu.jxust.agritrace.module.feedback.vo.FeedbackVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 消费者反馈 Mapper
 */
@Mapper
public interface ConsumerFeedbackMapper extends BaseMapper<ConsumerFeedback> {

    /**
     * 查询全部反馈列表
     *
     * @return 反馈列表
     */
    @Select("""
            SELECT
                id,
                batch_id,
                qr_id,
                feedback_type,
                content,
                contact_name,
                contact_phone,
                source_channel,
                status,
                is_public,
                handled_by,
                handled_at,
                handle_result,
                created_at
            FROM consumer_feedback
            ORDER BY created_at DESC, id DESC
            """)
    List<FeedbackVO> selectAllList();

    /**
     * 根据ID查询反馈详情
     *
     * @param id 反馈ID
     * @return 反馈详情
     */
    @Select("""
            SELECT
                id,
                batch_id,
                qr_id,
                feedback_type,
                content,
                contact_name,
                contact_phone,
                source_channel,
                status,
                is_public,
                handled_by,
                handled_at,
                handle_result,
                created_at
            FROM consumer_feedback
            WHERE id = #{id}
            LIMIT 1
            """)
    FeedbackVO selectDetailById(@Param("id") Long id);

    /**
     * 企业端查询反馈列表
     *
     * @param companyId 企业ID
     * @return 反馈列表
     */
    @Select("""
            SELECT
                cf.id,
                cf.batch_id,
                cf.qr_id,
                cf.feedback_type,
                cf.content,
                cf.contact_name,
                cf.contact_phone,
                cf.source_channel,
                cf.status,
                cf.is_public,
                cf.handled_by,
                cf.handled_at,
                cf.handle_result,
                cf.created_at
            FROM consumer_feedback cf
            JOIN trace_batch tb ON cf.batch_id = tb.id
            WHERE tb.company_id = #{companyId}
            ORDER BY cf.created_at DESC, cf.id DESC
            """)
    List<FeedbackVO> selectEnterpriseList(@Param("companyId") Long companyId);

    /**
     * 企业端查询反馈详情
     *
     * @param id 反馈ID
     * @param companyId 企业ID
     * @return 反馈详情
     */
    @Select("""
            SELECT
                cf.id,
                cf.batch_id,
                cf.qr_id,
                cf.feedback_type,
                cf.content,
                cf.contact_name,
                cf.contact_phone,
                cf.source_channel,
                cf.status,
                cf.is_public,
                cf.handled_by,
                cf.handled_at,
                cf.handle_result,
                cf.created_at
            FROM consumer_feedback cf
            JOIN trace_batch tb ON cf.batch_id = tb.id
            WHERE cf.id = #{id}
              AND tb.company_id = #{companyId}
            LIMIT 1
            """)
    FeedbackVO selectEnterpriseDetail(@Param("id") Long id, @Param("companyId") Long companyId);
}