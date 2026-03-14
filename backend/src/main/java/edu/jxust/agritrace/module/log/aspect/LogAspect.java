package edu.jxust.agritrace.module.log.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.jxust.agritrace.common.log.OperationLogAnnotation;
import edu.jxust.agritrace.common.security.LoginUser;
import edu.jxust.agritrace.common.security.SecurityUtils;
import edu.jxust.agritrace.module.log.entity.OperationLog;
import edu.jxust.agritrace.module.log.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 操作日志切面
 * 增强：支持异常记录、参数脱敏、长度截断
 */
@Aspect
@Component
public class LogAspect {

    private final OperationLogService operationLogService;
    
    @Autowired
    private HttpServletRequest request;
    
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    // 敏感字段匹配正则 (JSON 格式)
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile("(\"(?:password|oldPassword|newPassword|confirmPassword|token|authorization)\"\\s*:\\s*\")([^\"]+)(\")", Pattern.CASE_INSENSITIVE);

    public LogAspect(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Pointcut("@annotation(operationLog)")
    public void pointcut(OperationLogAnnotation operationLog) {}

    /**
     * 正常返回后的日志记录
     */
    @AfterReturning(value = "pointcut(operationLog)", argNames = "joinPoint,operationLog")
    public void afterReturning(JoinPoint joinPoint, OperationLogAnnotation operationLog) {
        saveLog(joinPoint, operationLog, "SUCCESS", null);
    }

    /**
     * 抛出异常后的日志记录
     */
    @AfterThrowing(value = "pointcut(operationLog)", throwing = "e", argNames = "joinPoint,operationLog,e")
    public void afterThrowing(JoinPoint joinPoint, OperationLogAnnotation operationLog, Throwable e) {
        saveLog(joinPoint, operationLog, "FAIL", e);
    }

    /**
     * 核心保存逻辑
     */
    private void saveLog(JoinPoint joinPoint, OperationLogAnnotation operationLog, String status, Throwable e) {
        OperationLog log = new OperationLog();

        LoginUser user = null;
        try {
            user = SecurityUtils.getLoginUser();
        } catch (Exception ignored) {
            // 兼容未登录场景（如注册、前台反馈）
        }

        if (user != null) {
            log.setOperatorId(user.getUserId());
            log.setOperatorName(user.getUsername());
            log.setRoleCode(user.getRoleCode());
        }

        log.setModule(operationLog.module());
        log.setAction(operationLog.action());
        log.setTargetType(operationLog.targetType());

        log.setRequestPath(request.getRequestURI());
        log.setRequestMethod(request.getMethod());

        // 处理请求参数：序列化 -> 脱敏 -> 截断
        try {
            String params = serializeAndDesensitize(joinPoint.getArgs());
            log.setRequestParams(truncate(params));
        } catch (Exception serializeEx) {
            log.setRequestParams("[Log error: Serialization failed]");
        }

        log.setIp(request.getRemoteAddr());
        log.setUserAgent(request.getHeader("User-Agent"));
        log.setResultStatus(status);
        log.setCreatedAt(LocalDateTime.now());

        try {
            operationLogService.save(log);
        } catch (Exception ignored) {
            // 防止日志落库失败影响主业务逻辑
        }
    }

    /**
     * 序列化并对敏感字段进行脱敏
     */
    private String serializeAndDesensitize(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        try {
            // 只序列化非 Servlet 对象的参数
            java.util.List<Object> validArgs = new java.util.ArrayList<>();
            for (Object arg : args) {
                if (arg != null &&
                    !(arg instanceof jakarta.servlet.ServletRequest) &&
                    !(arg instanceof jakarta.servlet.ServletResponse) &&
                    !(arg instanceof org.springframework.web.multipart.MultipartFile)) {
                    validArgs.add(arg);
                }
            }
            if (validArgs.isEmpty()) {
                return "[]";
            }
            String json = objectMapper.writeValueAsString(validArgs);
            Matcher matcher = SENSITIVE_PATTERN.matcher(json);
            return matcher.replaceAll("$1******$3");
        } catch (Exception e) {
            return "[Log Serialization Error]";
        }
    }

    /**
     * 长度截断保护
     */
    private String truncate(String text) {
        if (text == null || text.length() <= 2000) {
            return text;
        }
        return text.substring(0, 2000) + "...(truncated)";
    }
}
