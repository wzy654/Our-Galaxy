package com.ourgalaxy.file;

import com.ourgalaxy.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalStorageService {

    private static final Set<String> ALLOWED_FORMATS = Set.of("JPEG", "PNG", "GIF");
    private static final int MAX_IMAGE_PIXELS = 40_000_000;

    private final Path root;
    private final long maxSizeBytes;

    public LocalStorageService(StorageProperties properties) {
        this.root = Path.of(properties.localRoot()).toAbsolutePath().normalize();
        this.maxSizeBytes = properties.maxSizeBytes();
    }

    public StoredFile store(MultipartFile multipartFile, Long coupleId) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "请选择要上传的图片");
        }
        if (multipartFile.getSize() > maxSizeBytes) {
            throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, "图片超过允许大小");
        }

        ImageMetadata metadata = readImage(multipartFile);
        String extension = switch (metadata.format()) {
            case "JPEG" -> "jpg";
            case "PNG" -> "png";
            case "GIF" -> "gif";
            default -> throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "不支持的图片格式");
        };
        String mimeType = "image/" + extension;
        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        String storageKey = coupleId + "/" + date.getYear() + "/" + String.format("%02d", date.getMonthValue())
                + "/" + UUID.randomUUID() + "." + extension;
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "文件存储路径无效");
        }

        try {
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = multipartFile.getInputStream();
                 DigestInputStream digestInput = new DigestInputStream(input, digest)) {
                Files.copy(digestInput, target);
            }
            return new StoredFile(
                    storageKey.replace('\\', '/'),
                    mimeType,
                    multipartFile.getSize(),
                    HexFormat.of().formatHex(digest.digest()),
                    metadata.width(),
                    metadata.height()
            );
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "图片保存失败，请稍后重试");
        }
    }

    public Path load(String storageKey) {
        Path path = root.resolve(storageKey).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "文件不存在");
        }
        return path;
    }

    public void deleteQuietly(String storageKey) {
        try {
            Files.deleteIfExists(root.resolve(storageKey).normalize());
        } catch (IOException ignored) {
        }
    }

    private ImageMetadata readImage(MultipartFile multipartFile) {
        try (InputStream input = multipartFile.getInputStream();
             ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "仅支持 JPEG、PNG 或 GIF 图片");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                String format = reader.getFormatName().toUpperCase();
                if (!ALLOWED_FORMATS.contains(format)) {
                    throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "仅支持 JPEG、PNG 或 GIF 图片");
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if ((long) width * height > MAX_IMAGE_PIXELS) {
                    throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "图片分辨率过大");
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "图片内容无法读取");
                }
                return new ImageMetadata(format, width, height);
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "图片内容无法读取");
        }
    }

    private record ImageMetadata(String format, int width, int height) {
    }

    public record StoredFile(
            String storageKey,
            String mimeType,
            long sizeBytes,
            String checksumSha256,
            int width,
            int height
    ) {
    }
}
