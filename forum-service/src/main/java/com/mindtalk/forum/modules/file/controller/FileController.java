package com.mindtalk.forum.modules.file.controller;

import com.mindtalk.common.model.Result;
import com.mindtalk.forum.common.utils.MinioUtils;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import jakarta.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

@Slf4j
@Tag(name = "文件上传")
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final MinioUtils minioUtils;
    private final MinioClient minioClient;

    private static final int MAX_WIDTH = 1920;
    private static final int THUMB_WIDTH = 400;
    private static final long MAX_SIZE = 10 * 1024 * 1024;

    @Operation(summary = "上传图片（自动压缩+WebP）")
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail(400, "文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.fail(400, "只支持上传图片");
        }

        if (file.getSize() > MAX_SIZE) {
            return Result.fail(400, "图片大小不能超过 10MB");
        }

        try {
            // Read original image
            BufferedImage original = ImageIO.read(file.getInputStream());
            if (original == null) {
                return Result.fail(400, "无法解析图片");
            }

            int origWidth = original.getWidth();
            int origHeight = original.getHeight();

            // Compress to max 1920px width (only if larger)
            byte[] compressedBytes;
            if (origWidth > MAX_WIDTH) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                Thumbnails.of(original)
                        .width(MAX_WIDTH)
                        .keepAspectRatio(true)
                        .outputFormat("jpg")
                        .outputQuality(0.82)
                        .toOutputStream(bos);
                compressedBytes = bos.toByteArray();
            } else {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                Thumbnails.of(original)
                        .width(origWidth)
                        .keepAspectRatio(true)
                        .outputFormat("jpg")
                        .outputQuality(0.85)
                        .toOutputStream(bos);
                compressedBytes = bos.toByteArray();
            }

            // Generate thumbnail (400px width, WebP where supported)
            ByteArrayOutputStream thumbBos = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(compressedBytes))
                    .width(THUMB_WIDTH)
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .outputQuality(0.75)
                    .toOutputStream(thumbBos);
            byte[] thumbBytes = thumbBos.toByteArray();

            // Generate unique filename base
            String baseName = "post/" + UUID.randomUUID().toString();

            // Upload compressed original
            String origKey = baseName + ".jpg";
            minioUtils.uploadBytes(compressedBytes, origKey, "image/jpeg");
            String originalUrl = minioUtils.getAccessUrl(origKey);

            // Upload thumbnail
            String thumbKey = baseName + "_thumb.jpg";
            minioUtils.uploadBytes(thumbBytes, thumbKey, "image/jpeg");
            String thumbnailUrl = minioUtils.getAccessUrl(thumbKey);

            log.info("[上传] 原图 {}x{} -> 压缩 {}KB, 缩略图 {}KB",
                    origWidth, origHeight, compressedBytes.length / 1024, thumbBytes.length / 1024);

            Map<String, String> data = new HashMap<>();
            data.put("url", originalUrl);
            data.put("thumbnailUrl", thumbnailUrl);
            data.put("key", origKey);
            return Result.ok(data);

        } catch (Exception e) {
            log.error("[上传] 图片处理失败", e);
            return Result.fail(500, "图片处理失败");
        }
    }

    @Operation(summary = "流式读取文件（绕过 MinIO 预签名 Host 校验）")
    @GetMapping("/stream")
    public void stream(@RequestParam("key") String objectKey, HttpServletResponse response) {
        try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioUtils.getBucket())
                .object(objectKey)
                .build())) {

            String contentType = "application/octet-stream";
            if (objectKey.endsWith(".png")) contentType = "image/png";
            else if (objectKey.endsWith(".jpg") || objectKey.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (objectKey.endsWith(".webp")) contentType = "image/webp";
            else if (objectKey.endsWith(".gif")) contentType = "image/gif";
            else if (objectKey.endsWith(".svg")) contentType = "image/svg+xml";

            response.setContentType(contentType);
            response.setHeader("Cache-Control", "public, max-age=86400");

            OutputStream out = response.getOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = stream.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            out.flush();
        } catch (Exception e) {
            log.error("[文件] 流式读取失败 key={}", objectKey, e);
            response.setStatus(404);
        }
    }
}
