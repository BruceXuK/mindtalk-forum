package com.mindtalk.forum.common.utils;

import com.mindtalk.forum.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * MinIO 文件上传工具
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtils {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Value("${minio.bucket:mindtalk}")
    private String bucket;

    public String getBucket() {
        return bucket;
    }

    @Value("${minio.presigned-expiry-seconds:3600}")
    private int presignedExpirySeconds;

    /**
     * 上传文件
     *
     * @param file      文件
     * @param directory 目录（如 avatar、post）
     * @return 对象 key
     */
    public String upload(MultipartFile file, String directory) {
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String objectKey = directory + "/" + UUID.randomUUID() + extension;

        try (InputStream stream = file.getInputStream()) {
            ensureBucketExists();

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(stream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            log.info("[MinIO] 文件上传成功 bucket={} key={}", bucket, objectKey);
            return objectKey;
        } catch (Exception e) {
            log.error("[MinIO] 文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 获取文件访问 URL（通过后端流代理，避免预签名 Host 校验问题）
     */
    public String getAccessUrl(String objectKey) {
        return "/api/files/stream?key=" + objectKey;
    }

    /**
     * 获取文件访问 URL（通过 nginx /storage 代理，避免暴露内网地址）
     * @deprecated 预签名 URL 在反向代理后有 Host header 校验问题，改用 {@link #getAccessUrl(String)}
     */
    @Deprecated
    public String getPresignedUrl(String objectKey) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .method(Method.GET)
                            .expiry(presignedExpirySeconds)
                            .build());
            return url.replace(minioConfig.getInternalEndpoint(), "/storage");
        } catch (Exception e) {
            log.error("[MinIO] 获取预签名 URL 失败 key={}", objectKey, e);
            return null;
        }
    }

    /**
     * 上传字节数组
     */
    public String uploadBytes(byte[] data, String objectKey, String contentType) {
        try {
            ensureBucketExists();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(new java.io.ByteArrayInputStream(data), data.length, -1)
                    .contentType(contentType)
                    .build());
            log.info("[MinIO] 字节上传成功 bucket={} key={} size={}KB", bucket, objectKey, data.length / 1024);
            return objectKey;
        } catch (Exception e) {
            log.error("[MinIO] 字节上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 删除文件
     */
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
            log.info("[MinIO] 文件删除成功 key={}", objectKey);
        } catch (Exception e) {
            log.error("[MinIO] 文件删除失败 key={}", objectKey, e);
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }
}
