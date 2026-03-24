package edu.jxust.agritrace.module.batch.entity;

import java.util.Arrays;

public enum RiskActionType {
    COMMENT("COMMENT", "Handling comment"),
    RECTIFICATION("RECTIFICATION", "Rectification record"),
    PROCESSING("PROCESSING", "Marked in progress"),
    RECTIFIED("RECTIFIED", "Rectification completed");

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
                .orElseThrow(() -> new IllegalArgumentException("unsupported risk action type: " + code));
    }
}
