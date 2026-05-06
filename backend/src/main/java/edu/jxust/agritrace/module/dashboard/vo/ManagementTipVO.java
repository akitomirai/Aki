package edu.jxust.agritrace.module.dashboard.vo;

public record ManagementTipVO(
        String type,
        String title,
        String description,
        String level,
        String targetRoute
) {
}
