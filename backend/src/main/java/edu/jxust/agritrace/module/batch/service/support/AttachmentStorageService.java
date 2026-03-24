package edu.jxust.agritrace.module.batch.service.support;

import edu.jxust.agritrace.config.TraceProperties;
import edu.jxust.agritrace.module.batch.entity.AttachmentBusinessType;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Component
public class AttachmentStorageService {

    private static final DateTimeFormatter FOLDER_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TraceProperties traceProperties;

    public AttachmentStorageService(TraceProperties traceProperties) {
        this.traceProperties = traceProperties;
    }

    public StoredAttachment store(MultipartFile file, AttachmentBusinessType businessType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String originalName = file.getOriginalFilename() == null ? "upload.bin" : Paths.get(file.getOriginalFilename()).getFileName().toString();
        String extension = resolveExtension(originalName);
        String relativePath = businessType.code() + "/" + FOLDER_FORMATTER.format(LocalDate.now()) + "/" + UUID.randomUUID() + extension;
        Path root = Paths.get(traceProperties.getAttachmentStorageDir()).toAbsolutePath().normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("非法文件路径");
        }
        try {
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("文件保存失败", exception);
        }
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        return new StoredAttachment(originalName, relativePath.replace('\\', '/'), contentType, file.getSize());
    }

    public Resource load(String relativePath) {
        Path root = Paths.get(traceProperties.getAttachmentStorageDir()).toAbsolutePath().normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root) || !Files.exists(target)) {
            throw new IllegalArgumentException("附件不存在");
        }
        return new FileSystemResource(target);
    }

    private String resolveExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            return "";
        }
        String extension = fileName.substring(index).toLowerCase(Locale.ROOT);
        return extension.length() > 16 ? "" : extension;
    }

    public record StoredAttachment(
            String fileName,
            String filePath,
            String contentType,
            long size
    ) {
    }
}
