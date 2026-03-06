package edu.jxust.agritrace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源映射：
 * - 将本地存储目录映射到 HTTP 路径（例如 /files/**）
 * 说明：用于文件下载/预览（报告PDF、图片等）
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.file.storage-path}")
    private String storagePath;

    @Value("${app.file.public-prefix:/files}")
    private String publicPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String prefix = publicPrefix.startsWith("/") ? publicPrefix : "/" + publicPrefix;
        if (!prefix.endsWith("/")) prefix = prefix + "/";

        // file: 形式必须是绝对路径
        String location = storagePath;
        if (!StringUtils.hasText(location)) {
            return;
        }
        if (!location.endsWith("/")) location = location + "/";
        registry.addResourceHandler(prefix + "**")
                .addResourceLocations("file:" + location);
    }
}