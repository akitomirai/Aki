package edu.jxust.agritrace.common;

public enum ResultCode {
    SUCCESS(0, "success"),
    NOT_FOUND(404, "数据不存在"),
    SYSTEM_ERROR(500, "系统异常");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() { return code; }
    public String message() { return message; }
}
