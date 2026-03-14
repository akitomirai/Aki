package edu.jxust.agritrace.module.feedback.service;

import edu.jxust.agritrace.module.feedback.dto.FeedbackCreateDTO;
import edu.jxust.agritrace.module.feedback.dto.FeedbackHandleDTO;
import edu.jxust.agritrace.module.feedback.vo.FeedbackVO;

import java.util.List;

/**
 * 消费者反馈服务接口
 */
public interface ConsumerFeedbackService {

    /**
     * 前台提交反馈
     *
     * @param dto 提交参数
     * @return 反馈ID
     */
    Long create(FeedbackCreateDTO dto);

    /**
     * 后台查询反馈列表
     *
     * @return 反馈列表
     */
    List<FeedbackVO> list();

    /**
     * 后台查询反馈详情
     *
     * @param id 反馈ID
     * @return 反馈详情
     */
    FeedbackVO detail(Long id);

    /**
     * 后台处理反馈
     *
     * @param dto 处理参数
     */
    void handle(FeedbackHandleDTO dto);

    /**
     * 后台公开反馈
     *
     * @param id 反馈ID
     */
    void publish(Long id);

    /**
     * 企业端查询本企业反馈列表
     *
     * @param companyId 企业ID
     * @return 反馈列表
     */
    List<FeedbackVO> selectEnterpriseList(Long companyId);

    /**
     * 企业端查询本企业反馈详情
     *
     * @param id 反馈ID
     * @param companyId 企业ID
     * @return 反馈详情
     */
    FeedbackVO selectEnterpriseDetail(Long id, Long companyId);

    /**
     * 企业端处理反馈
     *
     * @param dto 处理参数
     * @param companyId 企业ID
     */
    void handleByCompanyId(FeedbackHandleDTO dto, Long companyId);
}