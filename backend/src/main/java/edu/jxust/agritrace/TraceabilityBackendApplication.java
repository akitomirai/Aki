package edu.jxust.agritrace;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
@MapperScan("edu.jxust.agritrace.module.batch.mapper")
public class TraceabilityBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TraceabilityBackendApplication.class, args);
    }
}
