package edu.jxust.agritrace.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件（MP 会自动识别数据库类型；如需可显式指定 DbType.MYSQL）
        //interceptor.addInnerInterceptor(new PaginationInnerInterceptor());

        return interceptor;
    }
}
