package edu.jxust.agritrace;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类
 */
@SpringBootApplication
@MapperScan("edu.jxust.agritrace.module.**.mapper")
public class TraceabilityBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TraceabilityBackendApplication.class, args);
    }
}