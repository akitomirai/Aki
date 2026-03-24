package edu.jxust.agritrace.module.batch.vo;

public record BatchActionVO(
        String code,
        String label,
        boolean enabled,
        String hint,
        String variant
) {
}
