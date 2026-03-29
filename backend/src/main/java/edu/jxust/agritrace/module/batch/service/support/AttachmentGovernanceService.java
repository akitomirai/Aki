package edu.jxust.agritrace.module.batch.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import edu.jxust.agritrace.config.TraceProperties;
import edu.jxust.agritrace.module.batch.mapper.BizAttachmentMapper;
import edu.jxust.agritrace.module.batch.mapper.po.BizAttachmentPO;
import edu.jxust.agritrace.module.batch.vo.AttachmentCleanupResultVO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class AttachmentGovernanceService {

    private final BizAttachmentMapper bizAttachmentMapper;
    private final AttachmentStorageService attachmentStorageService;
    private final TraceProperties traceProperties;

    public AttachmentGovernanceService(
            BizAttachmentMapper bizAttachmentMapper,
            AttachmentStorageService attachmentStorageService,
            TraceProperties traceProperties
    ) {
        this.bizAttachmentMapper = bizAttachmentMapper;
        this.attachmentStorageService = attachmentStorageService;
        this.traceProperties = traceProperties;
    }

    public AttachmentCleanupResultVO cleanupExpiredOrphans() {
        LocalDateTime deadline = LocalDateTime.now().minusHours(traceProperties.getAttachmentOrphanCleanupHours());
        List<BizAttachmentPO> expired = bizAttachmentMapper.selectList(new LambdaQueryWrapper<BizAttachmentPO>()
                .isNull(BizAttachmentPO::getBusinessId)
                .le(BizAttachmentPO::getCreatedAt, deadline)
                .orderByAsc(BizAttachmentPO::getId));

        List<Long> cleanedIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();
        for (BizAttachmentPO attachment : expired) {
            try {
                if (!attachmentStorageService.delete(attachment.getFilePath())) {
                    failedIds.add(attachment.getId());
                    continue;
                }
                bizAttachmentMapper.deleteById(attachment.getId());
                cleanedIds.add(attachment.getId());
            } catch (Exception exception) {
                failedIds.add(attachment.getId());
            }
        }

        return new AttachmentCleanupResultVO(
                cleanedIds.size(),
                cleanedIds,
                failedIds.size(),
                failedIds
        );
    }
}
