package edu.jxust.agritrace.module.batch.entity;

import java.util.Locale;

public enum MasterDataStatus {
    ENABLED,
    DISABLED,
    ARCHIVED;

    public static MasterDataStatus fromCode(String value) {
        if (value == null || value.isBlank()) {
            return ENABLED;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "ENABLED", "ACTIVE" -> ENABLED;
            case "DISABLED", "INACTIVE" -> DISABLED;
            case "ARCHIVED" -> ARCHIVED;
            default -> throw new IllegalArgumentException("unsupported status: " + value);
        };
    }

    public String label() {
        return switch (this) {
            case ENABLED -> "Enabled";
            case DISABLED -> "Disabled";
            case ARCHIVED -> "Archived";
        };
    }

    public boolean selectable() {
        return this == ENABLED;
    }
}
