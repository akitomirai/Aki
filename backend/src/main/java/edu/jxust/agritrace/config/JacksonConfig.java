package edu.jxust.agritrace.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 配置：
 * - 将 Long / long 统一序列化为字符串
 * - 解决前端 JS Number 精度丢失问题
 */
@Configuration
public class JacksonConfig {

    /**
     * Long 转 String 序列化模块
     */
    @Bean
    public Module longToStringModule() {
        SimpleModule module = new SimpleModule();

        // Long 包装类
        module.addSerializer(Long.class, ToStringSerializer.instance);
        // long 基本类型
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);

        return module;
    }
}