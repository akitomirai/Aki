package edu.jxust.agritrace;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(
        exclude = {
                DataSourceAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class,
                MybatisPlusAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        }
)
public class TraceabilityBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TraceabilityBackendApplication.class, args);
    }
}
