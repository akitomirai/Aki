package edu.jxust.agritrace.module.batch.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.jxust.agritrace.common.exception.BizException;
import edu.jxust.agritrace.common.security.SecurityUtils;
import edu.jxust.agritrace.module.batch.dto.BatchParticipantItemDTO;
import edu.jxust.agritrace.module.batch.dto.BatchParticipantSaveDTO;
import edu.jxust.agritrace.module.batch.entity.TraceBatch;
import edu.jxust.agritrace.module.batch.entity.TraceBatchParticipant;
import edu.jxust.agritrace.module.batch.mapper.TraceBatchMapper;
import edu.jxust.agritrace.module.batch.mapper.TraceBatchParticipantMapper;
import edu.jxust.agritrace.module.batch.service.BatchParticipantService;
import edu.jxust.agritrace.module.batch.vo.BatchParticipantVO;
import edu.jxust.agritrace.module.company.mapper.OrgCompanyMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class BatchParticipantServiceImpl implements BatchParticipantService {

    private static final Set<String> ALLOWED_BIZ_ROLES = Set.of(
            "PRODUCER",
            "FARMER",
            "TRANSPORTER",
            "PROCESSOR",
            "SELLER",
            "WAREHOUSE"
    );

    private final TraceBatchMapper traceBatchMapper;
    private final TraceBatchParticipantMapper traceBatchParticipantMapper;
    private final OrgCompanyMapper orgCompanyMapper;

    public BatchParticipantServiceImpl(TraceBatchMapper traceBatchMapper,
                                       TraceBatchParticipantMapper traceBatchParticipantMapper,
                                       OrgCompanyMapper orgCompanyMapper) {
        this.traceBatchMapper = traceBatchMapper;
        this.traceBatchParticipantMapper = traceBatchParticipantMapper;
        this.orgCompanyMapper = orgCompanyMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveForEnterprise(BatchParticipantSaveDTO dto) {
        TraceBatch batch = ensureBatchExists(dto.getBatchId());
        Long companyId = SecurityUtils.getCompanyId();
        if (companyId == null) {
            throw new BizException("当前用户未绑定企业");
        }
        if (!companyId.equals(batch.getCompanyId())) {
            throw new BizException("无权维护该批次参与主体");
        }
        replaceParticipants(batch, dto.getParticipants());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveForPlatform(BatchParticipantSaveDTO dto) {
        TraceBatch batch = ensureBatchExists(dto.getBatchId());
        replaceParticipants(batch, dto.getParticipants());
    }

    @Override
    public List<BatchParticipantVO> listByBatchId(Long batchId) {
        if (batchId == null) {
            throw new BizException("批次ID不能为空");
        }
        return traceBatchParticipantMapper.selectByBatchId(batchId);
    }

    private TraceBatch ensureBatchExists(Long batchId) {
        if (batchId == null) {
            throw new BizException("批次ID不能为空");
        }
        TraceBatch batch = traceBatchMapper.selectById(batchId);
        if (batch == null) {
            throw new BizException("批次不存在或已删除");
        }
        return batch;
    }

    private void replaceParticipants(TraceBatch batch, List<BatchParticipantItemDTO> items) {
        List<BatchParticipantItemDTO> normalizedItems = normalizeItems(items);

        traceBatchParticipantMapper.delete(Wrappers.<TraceBatchParticipant>lambdaQuery()
                .eq(TraceBatchParticipant::getBatchId, batch.getId()));

        List<TraceBatchParticipant> records = new ArrayList<>();
        if (batch.getCompanyId() != null) {
            TraceBatchParticipant creatorRecord = new TraceBatchParticipant();
            creatorRecord.setBatchId(batch.getId());
            creatorRecord.setCompanyId(batch.getCompanyId());
            creatorRecord.setBizRole("PRODUCER");
            creatorRecord.setStageOrder(0);
            creatorRecord.setIsCreator(true);
            creatorRecord.setRemark("批次创建企业");
            records.add(creatorRecord);
        }

        for (BatchParticipantItemDTO item : normalizedItems) {
            if (batch.getCompanyId() != null
                    && batch.getCompanyId().equals(item.getCompanyId())
                    && "PRODUCER".equals(item.getBizRole())) {
                continue;
            }
            TraceBatchParticipant record = new TraceBatchParticipant();
            record.setBatchId(batch.getId());
            record.setCompanyId(item.getCompanyId());
            record.setBizRole(item.getBizRole());
            record.setStageOrder(item.getStageOrder());
            record.setIsCreator(item.getIsCreator() != null && item.getIsCreator());
            record.setRemark(item.getRemark());
            records.add(record);
        }

        for (TraceBatchParticipant record : records) {
            traceBatchParticipantMapper.insert(record);
        }
    }

    private List<BatchParticipantItemDTO> normalizeItems(List<BatchParticipantItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        List<BatchParticipantItemDTO> normalizedItems = new ArrayList<>();
        LinkedHashSet<String> dedup = new LinkedHashSet<>();
        for (BatchParticipantItemDTO item : items) {
            if (item == null || item.getCompanyId() == null) {
                continue;
            }
            if (orgCompanyMapper.selectById(item.getCompanyId()) == null) {
                throw new BizException("参与企业不存在: " + item.getCompanyId());
            }

            String role = item.getBizRole();
            if (role == null || role.isBlank()) {
                throw new BizException("参与环节不能为空");
            }
            String normalizedRole = role.trim().toUpperCase();
            if (!ALLOWED_BIZ_ROLES.contains(normalizedRole)) {
                throw new BizException("非法参与环节: " + role);
            }

            String dedupKey = item.getCompanyId() + "|" + normalizedRole;
            if (!dedup.add(dedupKey)) {
                continue;
            }

            BatchParticipantItemDTO normalized = new BatchParticipantItemDTO();
            normalized.setCompanyId(item.getCompanyId());
            normalized.setBizRole(normalizedRole);
            normalized.setStageOrder(item.getStageOrder());
            normalized.setIsCreator(item.getIsCreator());
            normalized.setRemark(item.getRemark());
            normalizedItems.add(normalized);
        }
        return normalizedItems;
    }
}
