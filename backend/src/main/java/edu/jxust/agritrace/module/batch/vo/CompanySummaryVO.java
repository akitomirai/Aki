package edu.jxust.agritrace.module.batch.vo;

public record CompanySummaryVO(
        Long id,
        String name,
        String licenseNo,
        String contactName,
        String contactPhone,
        String address
) {
}
