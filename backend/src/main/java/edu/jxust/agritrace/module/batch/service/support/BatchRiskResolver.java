package edu.jxust.agritrace.module.batch.service.support;

import edu.jxust.agritrace.module.batch.entity.BatchEntity;
import edu.jxust.agritrace.module.batch.entity.BatchStatus;
import edu.jxust.agritrace.module.batch.entity.QualityReportEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;

@Component
public class BatchRiskResolver {

    public RiskSnapshot resolve(BatchEntity batch) {
        QualityReportEntity latestQuality = batch.getQualityReports().stream()
                .max(Comparator.comparing(QualityReportEntity::reportTime))
                .orElse(null);

        if (batch.getStatus() == BatchStatus.RECALLED) {
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
                    "The latest quality report is not pass. Review the batch before any release action.",
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
