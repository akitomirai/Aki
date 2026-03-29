package edu.jxust.agritrace.module.batch.service.support;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import edu.jxust.agritrace.config.TraceProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Component
public class QrImageStorageService {

    private final Path qrStorageDir;

    public QrImageStorageService(TraceProperties traceProperties) {
        this.qrStorageDir = Paths.get(traceProperties.getQrStorageDir()).toAbsolutePath().normalize();
    }

    public Path ensureQrImage(String token, String content) {
        try {
            Files.createDirectories(qrStorageDir);
            Path target = qrStorageDir.resolve(token + ".png");
            if (Files.exists(target)) {
                return target;
            }
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    320,
                    320,
                    Map.of(EncodeHintType.MARGIN, 1)
            );
            try (OutputStream outputStream = Files.newOutputStream(target)) {
                MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            }
            return target;
        } catch (IOException | WriterException exception) {
            throw new IllegalStateException("二维码图片生成失败", exception);
        }
    }

    public Resource loadQrImage(String token) {
        Path file = qrStorageDir.resolve(token + ".png");
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("未找到对应二维码图片");
        }
        return new FileSystemResource(file);
    }
}
