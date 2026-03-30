package edu.jxust.agritrace.module.batch.entity;

import java.util.Arrays;

public enum RiskActionType {
    COMMENT("COMMENT", "处理说明"),
    RECTIFICATION("RECTIFICATION", "整改记录"),
    PROCESSING("PROCESSING", "处理中"),
    RECTIFIED("RECTIFIED", "已完成整改");

    private final String code;
    private final String label;

    RiskActionType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static RiskActionType fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的风险处理类型: " + code));
    }
}
