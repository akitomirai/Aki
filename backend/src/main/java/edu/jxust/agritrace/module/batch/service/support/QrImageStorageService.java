package edu.jxust.agritrace.module.batch.service.support;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.QRCodeWriter;
import edu.jxust.agritrace.config.TraceProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

@Component
public class QrImageStorageService {

    private static final String STYLE_VERSION = "branded-blue-v1";
    private static final int QR_SIZE = 320;
    private static final int QR_DARK_COLOR = 0xFF1F3F68;
    private static final int QR_LIGHT_COLOR = 0xFFFFFFFF;
    private static final Color LOGO_BORDER_COLOR = new Color(108, 168, 217, 92);
    private static final String LOGO_RESOURCE = "qr-brand/system-icon.jpg";

    private final Path qrStorageDir;

    public QrImageStorageService(TraceProperties traceProperties) {
        this.qrStorageDir = Paths.get(traceProperties.getQrStorageDir()).toAbsolutePath().normalize();
    }

    public Path ensureQrImage(String token, String content) {
        try {
            Files.createDirectories(qrStorageDir);
            Path target = qrStorageDir.resolve(token + ".png");
            Path metadataTarget = qrStorageDir.resolve(token + ".txt");
            String normalizedContent = content == null ? "" : content;
            String metadata = STYLE_VERSION + System.lineSeparator() + normalizedContent;
            if (Files.exists(target)
                    && Files.exists(metadataTarget)
                    && metadata.equals(Files.readString(metadataTarget))) {
                return target;
            }
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(
                    normalizedContent,
                    BarcodeFormat.QR_CODE,
                    QR_SIZE,
                    QR_SIZE,
                    Map.of(
                            EncodeHintType.MARGIN, 1,
                            EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H
                    )
            );
            ImageIO.write(renderBrandedQrImage(matrix), "PNG", target.toFile());
            Files.writeString(
                    metadataTarget,
                    metadata,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            return target;
        } catch (IOException | WriterException exception) {
            throw new IllegalStateException("二维码图片生成失败", exception);
        }
    }

    private BufferedImage renderBrandedQrImage(BitMatrix matrix) throws IOException {
        BufferedImage image = new BufferedImage(QR_SIZE, QR_SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < QR_SIZE; y++) {
            for (int x = 0; x < QR_SIZE; x++) {
                image.setRGB(x, y, matrix.get(x, y) ? QR_DARK_COLOR : QR_LIGHT_COLOR);
            }
        }

        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            drawCenterLogo(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private void drawCenterLogo(Graphics2D graphics) throws IOException {
        int plateSize = Math.round(QR_SIZE * 0.27f);
        int plateX = Math.round((QR_SIZE - plateSize) / 2f);
        int plateY = Math.round((QR_SIZE - plateSize) / 2f);
        int plateRadius = Math.round(plateSize * 0.22f);
        int logoSize = Math.round(plateSize * 0.64f);
        int logoX = Math.round((QR_SIZE - logoSize) / 2f);
        int logoY = Math.round((QR_SIZE - logoSize) / 2f);
        int logoRadius = Math.round(logoSize * 0.18f);

        graphics.setColor(Color.WHITE);
        graphics.fillRoundRect(plateX, plateY, plateSize, plateSize, plateRadius, plateRadius);
        graphics.setColor(LOGO_BORDER_COLOR);
        graphics.setStroke(new BasicStroke(Math.max(2f, QR_SIZE * 0.006f)));
        graphics.drawRoundRect(plateX, plateY, plateSize, plateSize, plateRadius, plateRadius);

        BufferedImage logo = loadLogoImage();
        int sourceSize = Math.min(logo.getWidth(), logo.getHeight());
        int sourceX = Math.round((logo.getWidth() - sourceSize) / 2f);
        int sourceY = Math.round((logo.getHeight() - sourceSize) / 2f);
        Graphics2D clipped = (Graphics2D) graphics.create();
        try {
            clipped.setClip(new java.awt.geom.RoundRectangle2D.Float(
                    logoX,
                    logoY,
                    logoSize,
                    logoSize,
                    logoRadius,
                    logoRadius
            ));
            clipped.drawImage(
                    logo,
                    logoX,
                    logoY,
                    logoX + logoSize,
                    logoY + logoSize,
                    sourceX,
                    sourceY,
                    sourceX + sourceSize,
                    sourceY + sourceSize,
                    null
            );
        } finally {
            clipped.dispose();
        }
    }

    private BufferedImage loadLogoImage() throws IOException {
        ClassPathResource resource = new ClassPathResource(LOGO_RESOURCE);
        try (InputStream inputStream = resource.getInputStream()) {
            return ImageIO.read(inputStream);
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
