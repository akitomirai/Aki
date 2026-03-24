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
import java.util.Set;
import java.util.UUID;

@Component
public class AttachmentStorageService {

    private static final DateTimeFormatter FOLDER_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Set<String> TRACE_IMAGE_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".webp", ".gif");
    private static final Set<String> TRACE_IMAGE_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/webp", "image/gif");
    private static final Set<String> QUALITY_ATTACHMENT_EXTENSIONS = Set.of(".pdf", ".png", ".jpg", ".jpeg", ".webp");
    private static final Set<String> QUALITY_ATTACHMENT_CONTENT_TYPES = Set.of("application/pdf", "image/png", "image/jpeg", "image/webp");

    private final TraceProperties traceProperties;

    public AttachmentStorageService(TraceProperties traceProperties) {
        this.traceProperties = traceProperties;
    }

    public StoredAttachment store(MultipartFile file, AttachmentBusinessType businessType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("uploaded file cannot be empty");
        }

        String originalName = file.getOriginalFilename() == null
                ? "upload.bin"
                : Paths.get(file.getOriginalFilename()).getFileName().toString();
        String extension = resolveExtension(originalName);
        validate(file, businessType, extension);

        String relativePath = businessType.code() + "/" + FOLDER_FORMATTER.format(LocalDate.now()) + "/" + UUID.randomUUID() + extension;
        Path root = Paths.get(traceProperties.getAttachmentStorageDir()).toAbsolutePath().normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("illegal attachment path");
        }

        try {
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("failed to save attachment", exception);
        }

        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        return new StoredAttachment(originalName, relativePath.replace('\\', '/'), contentType, file.getSize());
    }

    public Resource load(String relativePath) {
        Path root = Paths.get(traceProperties.getAttachmentStorageDir()).toAbsolutePath().normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root) || !Files.exists(target)) {
            throw new IllegalArgumentException("attachment does not exist");
        }
        return new FileSystemResource(target);
    }

    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        Path root = Paths.get(traceProperties.getAttachmentStorageDir()).toAbsolutePath().normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
        }
    }

    private void validate(MultipartFile file, AttachmentBusinessType businessType, String extension) {
        long size = file.getSize();
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);

        if (businessType == AttachmentBusinessType.TRACE_IMAGE) {
            if (size > traceProperties.getTraceImageMaxSize().toBytes()) {
                throw new IllegalArgumentException("trace image exceeds the 5 MB limit");
            }
            if (!TRACE_IMAGE_EXTENSIONS.contains(extension) || !TRACE_IMAGE_CONTENT_TYPES.contains(contentType)) {
                throw new IllegalArgumentException("trace image only supports PNG, JPG, JPEG, WEBP or GIF");
            }
            return;
        }

        if (size > traceProperties.getQualityAttachmentMaxSize().toBytes()) {
            throw new IllegalArgumentException("quality attachment exceeds the 10 MB limit");
        }
        if (!QUALITY_ATTACHMENT_EXTENSIONS.contains(extension) || !QUALITY_ATTACHMENT_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("quality attachment only supports PDF, PNG, JPG, JPEG or WEBP");
        }
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
