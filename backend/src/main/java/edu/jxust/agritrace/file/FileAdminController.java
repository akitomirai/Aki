package edu.jxust.agritrace.file;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传接口（后台）
 * 用途：上传质检报告 PDF、图片等，返回可访问 URL。
 */
@RestController
@RequestMapping("/api/admin/file")
public class FileAdminController {

    @Value("${app.file.storage-path}")
    private String storagePath;

    @Value("${app.file.public-prefix:/files}")
    private String publicPrefix;

    /**
     * 上传文件：
     * - form-data: file=<文件>
     * - 可选参数：biz=quality/event (用于分目录)
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "biz", required = false, defaultValue = "common") String biz,
                                      HttpServletRequest request) throws Exception {

        if (file.isEmpty()) {
            return Map.of("code", -1, "message", "文件为空");
        }

        // 1) 生成保存目录：uploads/{biz}/{yyyy-MM-dd}/
        String day = LocalDate.now().toString();
        File dir = new File(storagePath, biz + "/" + day);
        if (!dir.exists() && !dir.mkdirs()) {
            return Map.of("code", -1, "message", "创建目录失败");
        }

        // 2) 生成安全文件名（避免原名冲突/路径注入）
        String original = file.getOriginalFilename();
        String ext = "";
        if (StringUtils.hasText(original) && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }
        String saveName = UUID.randomUUID().toString().replace("-", "") + ext;

        File target = new File(dir, saveName);

        // 3) 保存文件
        Files.copy(file.getInputStream(), target.toPath());

        // 4) 生成可访问 URL（/files/...）
        String prefix = publicPrefix.startsWith("/") ? publicPrefix : "/" + publicPrefix;
        String relativePath = biz + "/" + day + "/" + saveName;
        String url = prefix + "/" + relativePath;

        return Map.of(
                "code", 0,
                "message", "ok",
                "data", Map.of(
                        "fileName", original,
                        "url", url,
                        "size", file.getSize()
                )
        );
    }
}