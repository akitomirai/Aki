package edu.jxust.agritrace.common.exception;

import edu.jxust.agritrace.common.api.Result;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        return Result.fail(400, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e) {
        return Result.fail(403, "权限不足，拒绝访问");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = "参数校验失败";
        if (e.getBindingResult().hasFieldErrors()) {
            message = e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        }
        return Result.fail(400, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return Result.fail(400, "请求参数格式不正确");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        e.printStackTrace();
        return Result.fail(500, "系统异常，请稍后重试");
    }
}
