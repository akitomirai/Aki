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
import java.util.LinkedHashSet;
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
        if (file == null || file.getSize() == 0 || file.isEmpty()) {
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

    public boolean delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return true;
        }
        Path root = Paths.get(traceProperties.getAttachmentStorageDir()).toAbsolutePath().normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            return false;
        }
        try {
            return !Files.exists(target) || Files.deleteIfExists(target);
        } catch (IOException ignored) {
            return false;
        }
    }

    private void validate(MultipartFile file, AttachmentBusinessType businessType, String extension) {
        long size = file.getSize();
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        AttachmentRules rules = resolveRules(businessType);

        if (size > rules.maxBytes()) {
            throw new IllegalArgumentException(rules.displayName() + " exceeds the " + rules.maxLabel() + " limit");
        }
        if (!rules.allowedExtensions().contains(extension) || !rules.allowedContentTypes().contains(contentType)) {
            throw new IllegalArgumentException(rules.displayName() + " only supports " + rules.allowedLabels());
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

    private AttachmentRules resolveRules(AttachmentBusinessType businessType) {
        if (businessType == AttachmentBusinessType.TRACE_IMAGE) {
            return new AttachmentRules(
                    "trace image",
                    traceProperties.getTraceImageMaxSize().toBytes(),
                    traceProperties.getTraceImageMaxSize().toMegabytes() + " MB",
                    normalizeList(traceProperties.getTraceImageAllowedExtensions()),
                    normalizeList(traceProperties.getTraceImageAllowedContentTypes()),
                    formatAllowedLabels(traceProperties.getTraceImageAllowedExtensions())
            );
        }
        return new AttachmentRules(
                "quality attachment",
                traceProperties.getQualityAttachmentMaxSize().toBytes(),
                traceProperties.getQualityAttachmentMaxSize().toMegabytes() + " MB",
                normalizeList(traceProperties.getQualityAttachmentAllowedExtensions()),
                normalizeList(traceProperties.getQualityAttachmentAllowedContentTypes()),
                formatAllowedLabels(traceProperties.getQualityAttachmentAllowedExtensions())
        );
    }

    private LinkedHashSet<String> normalizeList(java.util.List<String> values) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        if (values == null) {
            return normalized;
        }
        values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .forEach(normalized::add);
        return normalized;
    }

    private String formatAllowedLabels(java.util.List<String> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            return "configured file types";
        }
        return extensions.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.replace(".", "").toUpperCase(Locale.ROOT))
                .distinct()
                .reduce((left, right) -> left + ", " + right)
                .orElse("configured file types");
    }

    private record AttachmentRules(
            String displayName,
            long maxBytes,
            String maxLabel,
            LinkedHashSet<String> allowedExtensions,
            LinkedHashSet<String> allowedContentTypes,
            String allowedLabels
    ) {
    }
}
