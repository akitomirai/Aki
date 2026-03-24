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
                        "Recall under handling",
                        defaultReason(latestRiskAction.reason(), batch.getStatusReason(), "The recalled batch is currently under handling."),
                        latestRiskAction.createdAt(),
                        "Do not purchase or consume this batch until the enterprise issues a new public notice."
                );
            }
            if (latestRiskAction != null && latestRiskAction.actionType() == RiskActionType.RECTIFIED) {
                return new RiskSnapshot(
                        true,
                        "RECTIFIED",
                        "pending",
                        "Recall rectification recorded",
                        defaultReason(latestRiskAction.reason(), batch.getStatusReason(), "Rectification has been recorded for the recalled batch."),
                        latestRiskAction.createdAt(),
                        "Wait for the enterprise or regulator to publish a final follow-up notice before any use."
                );
            }
            return new RiskSnapshot(
                    true,
                    "RECALLED",
                    "danger",
                    "Batch recalled",
                    defaultReason(batch.getStatusReason(), "This batch has been recalled. Stop sale and consumption."),
                    batch.getRecalledAt(),
                    "Do not purchase or consume this batch. Contact the company or regulator for disposal guidance."
            );
        }

        if (batch.getStatus() == BatchStatus.FROZEN) {
            if (latestRiskAction != null && latestRiskAction.actionType() == RiskActionType.PROCESSING) {
                return new RiskSnapshot(
                        true,
                        "PROCESSING",
                        "pending",
                        "Issue under handling",
                        defaultReason(latestRiskAction.reason(), batch.getStatusReason(), "The frozen batch is currently being handled."),
                        latestRiskAction.createdAt(),
                        "Keep the batch frozen until rectification is completed and resume is approved."
                );
            }
            if (latestRiskAction != null && latestRiskAction.actionType() == RiskActionType.RECTIFIED) {
                return new RiskSnapshot(
                        true,
                        "RECTIFIED",
                        "pending",
                        "Rectification completed",
                        defaultReason(latestRiskAction.reason(), batch.getStatusReason(), "Rectification has been completed and the batch is waiting for resume review."),
                        latestRiskAction.createdAt(),
                        "The batch is still frozen until the enterprise confirms resume conditions."
                );
            }
            return new RiskSnapshot(
                    true,
                    "FROZEN",
                    "warning",
                    "Batch frozen",
                    defaultReason(batch.getStatusReason(), "This batch is temporarily frozen for verification."),
                    batch.getFrozenAt(),
                    "Pause shipment and sale until the batch is reviewed and resumed."
            );
        }

        if (latestQuality != null && "FAIL".equalsIgnoreCase(latestQuality.result())) {
            return new RiskSnapshot(
                    true,
                    "RISK_PENDING",
                    "warning",
                    "Quality issue pending",
                    "The latest quality report did not pass. Review the batch before any release action.",
                    latestQuality.reportTime(),
                    "Keep this batch internal until the quality issue is corrected and rechecked."
            );
        }

        if (batch.getStatus() == BatchStatus.DRAFT) {
            return new RiskSnapshot(
                    false,
                    "PENDING",
                    "pending",
                    "Batch not published",
                    defaultReason(batch.getStatusReason(), "This batch is still a draft and not yet released to the public."),
                    null,
                    "Complete trace, quality and QR steps before publishing."
            );
        }

        return new RiskSnapshot(
                false,
                "NORMAL",
                "normal",
                "No public risk alert",
                "This batch is currently in a normal public trace state.",
                batch.getPublishedAt(),
                "Continue to check the timeline and quality summary if you need more context."
        );
    }

    public String resolveWorkbenchNode(BatchEntity batch) {
        BatchRiskActionEntity latestRiskAction = latestRiskActionAfterAbnormal(batch);
        if (batch.getStatus() == BatchStatus.RECALLED) {
            if (latestRiskAction != null && latestRiskAction.actionType() == RiskActionType.PROCESSING) {
                return "Recall handling in progress";
            }
            if (latestRiskAction != null && latestRiskAction.actionType() == RiskActionType.RECTIFIED) {
                return "Recall rectification recorded";
            }
            return "Recalled and exposed with a public risk alert";
        }
        if (batch.getStatus() == BatchStatus.FROZEN) {
            if (latestRiskAction != null && latestRiskAction.actionType() == RiskActionType.PROCESSING) {
                return "Frozen batch is under handling";
            }
            if (latestRiskAction != null && latestRiskAction.actionType() == RiskActionType.RECTIFIED) {
                return "Rectification completed, waiting for resume";
            }
            return "Frozen and waiting for handling";
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
            case "PROCESSING" -> "In processing";
            case "RECTIFIED" -> "Rectification completed";
            case "FROZEN" -> "Frozen";
            case "RECALLED" -> "Recalled";
            default -> "No active handling";
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
