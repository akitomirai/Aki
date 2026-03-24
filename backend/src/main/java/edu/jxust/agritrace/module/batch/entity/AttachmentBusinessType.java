package edu.jxust.agritrace.module.batch.entity;

import java.util.Arrays;

public enum AttachmentBusinessType {
    TRACE_IMAGE("trace-image"),
    QUALITY_ATTACHMENT("quality-attachment");

    private final String code;

    AttachmentBusinessType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static AttachmentBusinessType fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported businessType: " + code));
    }
}
