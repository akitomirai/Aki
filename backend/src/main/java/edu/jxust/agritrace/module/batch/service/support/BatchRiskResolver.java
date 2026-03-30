package edu.jxust.agritrace.module.batch.service.support;

import edu.jxust.agritrace.module.batch.entity.BatchEntity;
import edu.jxust.agritrace.module.batch.entity.BatchRiskActionEntity;
import edu.jxust.agritrace.module.batch.entity.BatchStatus;
import edu.jxust.agritrace.module.batch.entity.QualityReportEntity;
import edu.jxust.agritrace.module.batch.entity.RiskActionType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Component
public class BatchRiskResolver {

    public RiskSnapshot resolve(BatchEntity batch) {
        QualityReportEntity latestQuality = batch.getQualityReports().stream()
                .max(Comparator.comparing(QualityReportEntity::reportTime))
                .orElse(null);
        BatchRiskActionEntity latestRiskAction = latestRiskActionAfterAbnormal(batch);

        if (batch.getStatus() == BatchStatus.RECALLED) {
            if (latestRiskAction != null && latestRiskAction.actionType() == RiskActionType.PROCESSING) {
                return new RiskSnapshot(
                        true,
                        "PROCESSING",
                        "warning",
                        "召回处理中",
                        defaultReason(latestRiskAction.reason(), batch.getStatusReason(), "该批次已召回，当前正在处理中。"),
                        latestRiskAction.createdAt(),
                        "在企业或监管方发布进一步通知前，请勿继续购买或食用该批次。"
                );
            }
            if (latestRiskAction != null && latestRiskAction.actionType() == RiskActionType.RECTIFIED) {
                return new RiskSnapshot(
                        true,
                        "RECTIFIED",
                        "pending",
                        "召回整改已留痕",
                        defaultReason(latestRiskAction.reason(), batch.getStatusReason(), "召回批次已记录整改信息。"),
                        latestRiskAction.createdAt(),
                        "请等待企业或监管方发布最终处理结论后，再决定是否使用。"
                );
            }
            return new RiskSnapshot(
                    true,
                    "RECALLED",
                    "danger",
                    "批次已召回",
                    defaultReason(batch.getStatusReason(), "该批次已召回，请立即停止销售和食用。"),
                    batch.getRecalledAt(),
                    "请勿继续购买或食用该批次，必要时联系企业或监管部门获取处理建议。"
            );
        }

        if (batch.getStatus() == BatchStatus.FROZEN) {
            if (latestRiskAction != null && latestRiskAction.actionType() == RiskActionType.PROCESSING) {
                return new RiskSnapshot(
                        true,
                        "PROCESSING",
                        "pending",
                        "异常处理中",
                        defaultReason(latestRiskAction.reason(), batch.getStatusReason(), "该冻结批次当前正在处理中。"),
                        latestRiskAction.createdAt(),
                        "请保持冻结状态，待整改完成并确认恢复条件后再继续流通。"
                );
            }
            if (latestRiskAction != null && latestRiskAction.actionType() == RiskActionType.RECTIFIED) {
                return new RiskSnapshot(
                        true,
                        "RECTIFIED",
                        "pending",
                        "整改已完成",
                        defaultReason(latestRiskAction.reason(), batch.getStatusReason(), "整改已完成，等待恢复流通复核。"),
                        latestRiskAction.createdAt(),
                        "在企业确认恢复条件前，批次仍保持冻结状态。"
                );
            }
            return new RiskSnapshot(
                    true,
                    "FROZEN",
                    "warning",
                    "批次已冻结",
                    defaultReason(batch.getStatusReason(), "该批次因异常核查暂时冻结。"),
                    batch.getFrozenAt(),
                    "请暂停发运和销售，待复核结束后再决定是否恢复。"
            );
        }

        if (latestQuality != null && "FAIL".equalsIgnoreCase(latestQuality.result())) {
            return new RiskSnapshot(
                    true,
                    "RISK_PENDING",
                    "warning",
                    "质检异常待处理",
                    "最新质检结果未通过，批次在进一步处理前不建议对外发布。",
                    latestQuality.reportTime(),
                    "建议先完成整改和复检，再决定是否继续对外流通。"
            );
        }

        if (batch.getStatus() == BatchStatus.DRAFT) {
            return new RiskSnapshot(
                    false,
                    "PENDING",
                    "pending",
                    "批次尚未发布",
                    defaultReason(batch.getStatusReason(), "该批次仍处于草稿阶段，尚未对外发布。"),
                    null,
                    "建议先补齐追溯、质检和二维码信息后再发布。"
            );
        }

        return new RiskSnapshot(
                false,
                "NORMAL",
                "normal",
                "当前无风险提醒",
                "该批次当前处于正常公开查询状态。",
                batch.getPublishedAt(),
                "如需了解更多信息，可继续查看时间线和质检摘要。"
        );
    }

    public String resolveWorkbenchNode(BatchEntity batch) {
        BatchRiskActionEntity latestRiskAction = latestRiskActionAfterAbnormal(batch);
        if (batch.getStatus() == BatchStatus.RECALLED) {
            if (latestRiskAction != null && latestRiskAction.actionType() == RiskActionType.PROCESSING) {
                return "召回处理中";
            }
            if (latestRiskAction != null && latestRiskAction.actionType() == RiskActionType.RECTIFIED) {
                return "召回整改已留痕";
            }
            return "批次已召回，公开页同步展示风险提醒";
        }
        if (batch.getStatus() == BatchStatus.FROZEN) {
            if (latestRiskAction != null && latestRiskAction.actionType() == RiskActionType.PROCESSING) {
                return "冻结批次处理中";
            }
            if (latestRiskAction != null && latestRiskAction.actionType() == RiskActionType.RECTIFIED) {
                return "整改已完成，等待恢复";
            }
            return "批次已冻结，等待处理";
        }
        return null;
    }

    public boolean canResume(BatchEntity batch) {
        if (batch.getStatus() != BatchStatus.FROZEN) {
            return false;
        }
        LocalDateTime abnormalAt = abnormalAt(batch);
        List<BatchRiskActionEntity> latestActions = batch.getRiskActions().stream()
                .filter(item -> abnormalAt == null || !item.createdAt().isBefore(abnormalAt))
                .toList();
        boolean hasHandlingContext = latestActions.stream()
                .anyMatch(item -> item.actionType() == RiskActionType.COMMENT || item.actionType() == RiskActionType.RECTIFICATION);
        boolean hasRectified = latestActions.stream()
                .anyMatch(item -> item.actionType() == RiskActionType.RECTIFIED);
        return hasHandlingContext && hasRectified;
    }

    public String currentHandlingStage(BatchEntity batch) {
        if (batch.getStatus() != BatchStatus.FROZEN && batch.getStatus() != BatchStatus.RECALLED) {
            return "NORMAL";
        }
        BatchRiskActionEntity latestRiskAction = latestRiskActionAfterAbnormal(batch);
        if (latestRiskAction == null) {
            return batch.getStatus().name();
        }
        return switch (latestRiskAction.actionType()) {
            case PROCESSING -> "PROCESSING";
            case RECTIFIED -> "RECTIFIED";
            default -> batch.getStatus().name();
        };
    }

    public String currentHandlingStageLabel(BatchEntity batch) {
        return switch (currentHandlingStage(batch)) {
            case "PROCESSING" -> "处理中";
            case "RECTIFIED" -> "已完成整改";
            case "FROZEN" -> "已冻结";
            case "RECALLED" -> "已召回";
            default -> "当前无处理动作";
        };
    }

    private BatchRiskActionEntity latestRiskActionAfterAbnormal(BatchEntity batch) {
        LocalDateTime abnormalAt = abnormalAt(batch);
        return batch.getRiskActions().stream()
                .filter(item -> abnormalAt == null || !item.createdAt().isBefore(abnormalAt))
                .max(Comparator.comparing(BatchRiskActionEntity::createdAt))
                .orElse(null);
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

    private String defaultReason(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return fallback;
    }

    private String defaultReason(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public record RiskSnapshot(
            boolean hasRisk,
            String status,
            String riskLevel,
            String title,
            String reason,
            LocalDateTime updatedAt,
            String tip
    ) {
    }
}
