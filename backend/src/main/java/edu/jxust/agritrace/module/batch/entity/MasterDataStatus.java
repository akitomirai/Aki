package edu.jxust.agritrace.module.batch.entity;

import java.util.Locale;

public enum MasterDataStatus {
    ENABLED,
    DISABLED;

    public static MasterDataStatus fromCode(String value) {
        if (value == null || value.isBlank()) {
            return ENABLED;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "ENABLED", "ACTIVE" -> ENABLED;
            case "DISABLED", "INACTIVE" -> DISABLED;
            default -> throw new IllegalArgumentException("unsupported status: " + value);
        };
    }

    public String label() {
        return this == ENABLED ? "Enabled" : "Disabled";
    }
}
