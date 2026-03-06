package edu.jxust.agritrace.common.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 统一返回体 Result：
 * - 避免 Map.of(...) 的 null NPE
 * - 统一 code/message/data 输出格式，便于前端拦截与展示
 *
 * code 约定：
 * - 0: 成功
 * - 其他: 失败（业务错误/参数错误等）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 业务状态码：0成功，非0失败 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 返回数据（允许为 null） */
    private T data;

    /** 时间戳（毫秒）便于排查问题 */
    private long ts;

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data, Instant.now().toEpochMilli());
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(0, message, data, Instant.now().toEpochMilli());
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(-1, message, null, Instant.now().toEpochMilli());
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null, Instant.now().toEpochMilli());
    }
}