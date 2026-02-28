package edu.jxust.agritrace;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("edu.jxust.agritrace.mapper")
@SpringBootApplication
public class AgriTraceabilityApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgriTraceabilityApplication.class, args);
    }
}
