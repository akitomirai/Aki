package edu.jxust.agritrace.module.batch.service.support;

import edu.jxust.agritrace.module.batch.entity.BatchEntity;
import edu.jxust.agritrace.module.batch.entity.BatchRiskActionEntity;
import edu.jxust.agritrace.module.batch.entity.BatchStatus;
import edu.jxust.agritrace.module.batch.entity.RiskActionType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class BatchStatusFlowAdvisor {

    private static final Map<BatchStatus, Set<BatchStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(BatchStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(BatchStatus.DRAFT, Set.of(BatchStatus.PUBLISHED));
        ALLOWED_TRANSITIONS.put(BatchStatus.PUBLISHED, Set.of(BatchStatus.FROZEN, BatchStatus.RECALLED));
        ALLOWED_TRANSITIONS.put(BatchStatus.FROZEN, Set.of(BatchStatus.PUBLISHED, BatchStatus.RECALLED));
        ALLOWED_TRANSITIONS.put(BatchStatus.RECALLED, Set.of());
    }

    public boolean canTransition(BatchStatus currentStatus, BatchStatus targetStatus) {
        if (currentStatus == null || targetStatus == null || currentStatus == targetStatus || targetStatus == BatchStatus.DRAFT) {
            return false;
        }
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(targetStatus);
    }

    public String blockedTransitionHint(BatchStatus currentStatus, BatchStatus targetStatus) {
        if (targetStatus == null) {
            return "目标状态不能为空";
        }
        if (currentStatus == null) {
            return "当前批次状态异常，无法变更";
        }
        if (currentStatus == targetStatus) {
            return "当前状态与目标状态一致，无需重复操作";
        }
        if (targetStatus == BatchStatus.DRAFT) {
            return "不支持手动回退到草稿状态";
        }
        return switch (currentStatus) {
            case DRAFT -> "草稿批次只允许发布";
            case PUBLISHED -> "已发布批次只允许冻结或召回";
            case FROZEN -> "已冻结批次只允许恢复发布或召回";
            case RECALLED -> "已召回批次不可再变更状态";
        };
    }

    public RecommendedAction recommendAction(
            BatchEntity batch,
            boolean hasTraceRecord,
            boolean hasQualifiedQuality,
            boolean hasQr,
            boolean canResume
    ) {
        BatchStatus status = batch.getStatus();
        if (status == BatchStatus.DRAFT) {
            if (!hasTraceRecord) {
                return new RecommendedAction("ADD_TRACE", "补录追溯", "先补一条关键现场记录，再继续质检与发布链路。");
            }
            if (!hasQualifiedQuality) {
                return new RecommendedAction("UPLOAD_QUALITY", "上传质检", "补齐合格质检摘要，发布动作才会开放。");
            }
            if (!hasQr) {
                return new RecommendedAction("GENERATE_QR", "生成二维码", "先生成二维码，公开页入口才能生效。");
            }
            return new RecommendedAction("PUBLISH", "发布批次", "关键资料已齐，建议直接完成发布。");
        }
        if (status == BatchStatus.PUBLISHED) {
            return new RecommendedAction("VIEW_PUBLIC", "查看公开页", "已发布状态建议优先核对公开页展示效果。");
        }
        if (status == BatchStatus.RECALLED) {
            return recommendForRecalled(batch);
        }
        if (canResume) {
            return new RecommendedAction("RESUME", "恢复发布", "整改链路已闭环，建议恢复发布并回工作台复核。");
        }
        return recommendForFrozen(batch);
    }

    private RecommendedAction recommendForFrozen(BatchEntity batch) {
        List<BatchRiskActionEntity> effectiveActions = actionsAfterAbnormal(batch);
        boolean hasComment = effectiveActions.stream().anyMatch(item -> item.actionType() == RiskActionType.COMMENT);
        boolean hasRectification = effectiveActions.stream().anyMatch(item -> item.actionType() == RiskActionType.RECTIFICATION);
        BatchRiskActionEntity latestAction = effectiveActions.stream()
                .max(Comparator.comparing(BatchRiskActionEntity::createdAt))
                .orElse(null);

        if (!hasComment) {
            return new RecommendedAction("RISK_COMMENT", "补处理说明", "先说明风险范围与当前判断，后续动作更容易对齐。");
        }
        if (!hasRectification) {
            return new RecommendedAction("RISK_RECTIFICATION", "补整改记录", "补齐整改动作和责任留痕，再推进状态更新。");
        }
        if (latestAction != null && latestAction.actionType() == RiskActionType.PROCESSING) {
            return new RecommendedAction("RISK_RECTIFICATION", "补整改记录", "当前已标记处理中，建议继续补整改记录。");
        }
        return new RecommendedAction("RISK_RECTIFIED", "标记已整改", "整改信息已较完整，可推进到已整改。");
    }

    private RecommendedAction recommendForRecalled(BatchEntity batch) {
        List<BatchRiskActionEntity> effectiveActions = actionsAfterAbnormal(batch);
        boolean hasComment = effectiveActions.stream().anyMatch(item -> item.actionType() == RiskActionType.COMMENT);
        if (!hasComment) {
            return new RecommendedAction("RISK_COMMENT", "补处理说明", "召回态建议先补处理说明，明确范围和后续安排。");
        }
        return new RecommendedAction("RISK_RECTIFICATION", "补整改记录", "召回态建议持续补整改记录，便于监管回查。");
    }

    private List<BatchRiskActionEntity> actionsAfterAbnormal(BatchEntity batch) {
        LocalDateTime abnormalAt = abnormalAt(batch);
        return batch.getRiskActions().stream()
                .filter(item -> abnormalAt == null || !item.createdAt().isBefore(abnormalAt))
                .toList();
    }

    private LocalDateTime abnormalAt(BatchEntity batch) {
        if (batch.getStatus() == BatchStatus.RECALLED) {
            return batch.getRecalledAt();
        }
        if (batch.getStatus() == BatchStatus.FROZEN) {
            return batch.getFrozenAt();
        }
        return null;
    }

    public record RecommendedAction(String code, String label, String hint) {
    }
}
