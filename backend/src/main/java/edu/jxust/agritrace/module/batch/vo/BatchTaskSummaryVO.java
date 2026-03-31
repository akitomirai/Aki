package edu.jxust.agritrace.module.batch.vo;

public record BatchTaskSummaryVO(
        Long assigneeUserId,
        String assigneeName,
        String assignedAt,
        String taskStatus,
        String taskStatusLabel,
        String taskCompletedAt,
        boolean todayCompleted,
        boolean draftPending,
        String draftStatusLabel,
        String draftUpdatedAt
) {
}
