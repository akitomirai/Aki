package edu.jxust.agritrace.module.quality.service.impl;

import edu.jxust.agritrace.common.exception.BizException;
import edu.jxust.agritrace.common.security.SecurityUtils;
import edu.jxust.agritrace.common.util.NotarySnapshotBuilder;
import edu.jxust.agritrace.module.batch.entity.TraceBatch;
import edu.jxust.agritrace.module.batch.mapper.TraceBatchMapper;
import edu.jxust.agritrace.module.notary.constant.NotaryBizType;
import edu.jxust.agritrace.module.notary.service.HashNotaryService;
import edu.jxust.agritrace.module.quality.dto.QualityReportCreateDTO;
import edu.jxust.agritrace.module.quality.dto.QualityReportUpdateDTO;
import edu.jxust.agritrace.module.quality.entity.QualityReport;
import edu.jxust.agritrace.module.quality.mapper.QualityReportMapper;
import edu.jxust.agritrace.module.quality.service.QualityReportService;
import edu.jxust.agritrace.module.quality.vo.QualityReportVO;
import edu.jxust.agritrace.module.quality.vo.QualityVerifyVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QualityReportServiceImpl implements QualityReportService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final QualityReportMapper qualityReportMapper;
    private final TraceBatchMapper traceBatchMapper;
    private final HashNotaryService hashNotaryService;

    public QualityReportServiceImpl(QualityReportMapper qualityReportMapper,
                                    TraceBatchMapper traceBatchMapper,
                                    HashNotaryService hashNotaryService) {
        this.qualityReportMapper = qualityReportMapper;
        this.traceBatchMapper = traceBatchMapper;
        this.hashNotaryService = hashNotaryService;
    }

    /**
     * 新增质检报告
     * 规则：
     * 1. 批次必须存在
     * 2. 只能给自己企业的批次新增
     * 3. 冻结/召回批次不允许企业侧继续新增质检报告
     * 4. 报告编号、检测机构、检测结果不能为空
     * 5. 新增成功后写入摘要存证
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(QualityReportCreateDTO dto) {
        if (dto.getBatchId() == null) {
            throw new BizException("批次ID不能为空");
        }
        if (isBlank(dto.getReportNo())) {
            throw new BizException("报告编号不能为空");
        }
        if (isBlank(dto.getAgency())) {
            throw new BizException("检测机构不能为空");
        }
        if (isBlank(dto.getResult())) {
            throw new BizException("检测结果不能为空");
        }

        TraceBatch batch = traceBatchMapper.selectById(dto.getBatchId());
        if (batch == null) {
            throw new BizException("批次不存在或已删除");
        }

        Long companyId = SecurityUtils.getCompanyId();
        Long userId = SecurityUtils.getUserId();

        if (companyId == null || !companyId.equals(batch.getCompanyId())) {
            throw new BizException("无权为该批次新增质检报告");
        }

        if ("FROZEN".equals(batch.getStatus())) {
            throw new BizException("批次已冻结，不能新增质检报告");
        }
        if ("RECALLED".equals(batch.getStatus())) {
            throw new BizException("批次已召回，不能新增质检报告");
        }
        validateReportJson(dto.getReportJson());

        QualityReport report = new QualityReport();
        BeanUtils.copyProperties(dto, report);

        qualityReportMapper.insert(report);

        hashNotaryService.notarize(
                NotaryBizType.QUALITY_REPORT,
                report.getId(),
                NotarySnapshotBuilder.buildQualitySnapshot(report),
                userId,
                "质检报告存证"
        );

        return report.getId();
    }

    /**
     * 修改质检报告
     * 规则：
     * 1. 报告必须存在
     * 2. 关联批次必须存在
     * 3. 只能修改自己企业批次下的质检报告
     * 4. 冻结/召回批次不允许企业侧继续修改质检报告
     * 5. 仅在字段不为 null 时更新
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(QualityReportUpdateDTO dto) {
        if (dto.getId() == null) {
            throw new BizException("质检报告ID不能为空");
        }

        QualityReport report = qualityReportMapper.selectById(dto.getId());
        if (report == null) {
            throw new BizException("质检报告不存在");
        }

        TraceBatch batch = traceBatchMapper.selectById(report.getBatchId());
        if (batch == null) {
            throw new BizException("批次不存在或已删除");
        }

        Long companyId = SecurityUtils.getCompanyId();
        if (companyId == null || !companyId.equals(batch.getCompanyId())) {
            throw new BizException("无权修改该质检报告");
        }

        if ("FROZEN".equals(batch.getStatus())) {
            throw new BizException("批次已冻结，不能修改质检报告");
        }
        if ("RECALLED".equals(batch.getStatus())) {
            throw new BizException("批次已召回，不能修改质检报告");
        }

        if (dto.getReportNo() != null) {
            if (isBlank(dto.getReportNo())) {
                throw new BizException("报告编号不能为空");
            }
            report.setReportNo(dto.getReportNo());
        }
        if (dto.getAgency() != null) {
            if (isBlank(dto.getAgency())) {
                throw new BizException("检测机构不能为空");
            }
            report.setAgency(dto.getAgency());
        }
        if (dto.getResult() != null) {
            if (isBlank(dto.getResult())) {
                throw new BizException("检测结果不能为空");
            }
            report.setResult(dto.getResult());
        }
        if (dto.getReportFileUrl() != null) {
            report.setReportFileUrl(dto.getReportFileUrl());
        }
        if (dto.getReportJson() != null) {
            validateReportJson(dto.getReportJson());
            report.setReportJson(dto.getReportJson());
        }

        qualityReportMapper.updateById(report);
    }

    /**
     * 按批次查询质检报告列表
     * 规则：
     * 1. 批次必须存在
     * 2. 企业端只能查看自己企业批次下的质检报告
     */
    @Override
    public List<QualityReportVO> listByBatchId(Long batchId) {
        if (batchId == null) {
            throw new BizException("批次ID不能为空");
        }

        TraceBatch batch = traceBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BizException("批次不存在或已删除");
        }

        // 仅在已登录且角色为企业用户时校验企业 ID
        try {
            Long companyId = SecurityUtils.getCompanyId();
            if (companyId != null && !companyId.equals(batch.getCompanyId())) {
                throw new BizException("无权查看该批次的质检报告");
            }
        } catch (Exception e) {
            // 未登录或公开访问，跳过企业校验 (由 Controller 层控制公开访问权限)
        }

        return qualityReportMapper.selectByBatchId(batchId);
    }

    /**
     * 质检报告详情
     * 规则：
     * 1. 报告必须存在
     * 2. 企业端只能查看自己企业的质检报告
     */
    @Override
    public QualityReportVO detail(Long id) {
        if (id == null) {
            throw new BizException("质检报告ID不能为空");
        }

        QualityReport report = qualityReportMapper.selectById(id);
        if (report == null) {
            throw new BizException("质检报告不存在");
        }

        TraceBatch batch = traceBatchMapper.selectById(report.getBatchId());
        if (batch == null) {
            throw new BizException("批次不存在或已删除");
        }

        Long companyId = SecurityUtils.getCompanyId();
        if (companyId != null && !companyId.equals(batch.getCompanyId())) {
            throw new BizException("无权查看该质检报告");
        }

        QualityReportVO vo = new QualityReportVO();
        BeanUtils.copyProperties(report, vo);
        return vo;
    }

    /**
     * 查询批次最新质检结论
     * 规则：
     * 1. batchId 不能为空
     * 2. 批次必须存在
     * 3. 企业侧（companyId 非空）必须校验该批次属于当前企业
     * 4. 平台/监管（companyId 为空）按只读规则放行
     */
    @Override
    public QualityVerifyVO verifyLatestByBatchId(Long batchId) {
        if (batchId == null) {
            throw new BizException("批次ID不能为空");
        }

        TraceBatch batch = traceBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BizException("批次不存在或已删除");
        }

        // 仅在已登录且角色为企业用户时校验企业 ID
        try {
            Long companyId = SecurityUtils.getCompanyId();
            if (companyId != null && !companyId.equals(batch.getCompanyId())) {
                throw new BizException("无权查看该批次的最新质检结论");
            }
        } catch (Exception e) {
            // 未登录或公开访问，跳过企业校验
        }

        QualityReportVO latest = qualityReportMapper.selectLatestByBatchId(batchId);

        QualityVerifyVO vo = new QualityVerifyVO();
        vo.setBatchId(batchId);
        if (latest == null) {
            vo.setHasReport(false);
            vo.setLatestReportId(null);
            vo.setLatestResult(null);
        } else {
            vo.setHasReport(true);
            vo.setLatestReportId(latest.getId());
            vo.setLatestResult(latest.getResult());
        }
        return vo;
    }

    /**
     * 删除质检报告
     * 规则：
     * 1. 报告必须存在
     * 2. 关联批次必须存在
     * 3. 企业端只能删除自己企业的质检报告
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (id == null) {
            throw new BizException("质检报告ID不能为空");
        }

        QualityReport report = qualityReportMapper.selectById(id);
        if (report == null) {
            throw new BizException("质检报告不存在");
        }

        TraceBatch batch = traceBatchMapper.selectById(report.getBatchId());
        if (batch == null) {
            throw new BizException("批次不存在或已删除");
        }

        Long companyId = SecurityUtils.getCompanyId();
        if (companyId == null || !companyId.equals(batch.getCompanyId())) {
            throw new BizException("无权删除该质检报告");
        }

        qualityReportMapper.deleteById(id);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void validateReportJson(String reportJson) {
        if (isBlank(reportJson)) {
            return;
        }
        try {
            OBJECT_MAPPER.readTree(reportJson);
        } catch (Exception e) {
            throw new BizException("扩展信息格式不正确");
        }
    }
}
