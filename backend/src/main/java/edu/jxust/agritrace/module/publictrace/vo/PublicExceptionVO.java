package edu.jxust.agritrace.module.publictrace.vo;

public record PublicExceptionVO(
        boolean hasException,
        String level,
        String message,
        String suggestion
) {
}
