package edu.jxust.agritrace.module.batch.dto;

public record BatchAssignmentRequest(
        Long assigneeUserId,
        Boolean forceClearDraft
) {
}
