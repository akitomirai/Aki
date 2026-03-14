package edu.jxust.agritrace.common.log;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLogAnnotation {

    /**
     * 模块
     */
    String module();

    /**
     * 操作
     */
    String action();

    /**
     * 目标类型
     */
    String targetType() default "";
}