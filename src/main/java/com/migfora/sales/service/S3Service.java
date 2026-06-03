package com.migfora.sales.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 03/06/2026
 * @Time: 4:17 AM
 */
@Service
@Slf4j
public class S3Service {


    private final S3Client s3Client;

    @Value("${aws.s3.bucket:migfora-reports}")
    private String bucket;

    @Value("${aws.s3.presigned-url-expiry-minutes:60}")
    private int presignedUrlExpiryMinutes;

    private final S3Presigner presigner;

    public S3Service(S3Client s3Client, S3Presigner presigner) {
        this.s3Client  = s3Client;
        this.presigner = presigner;
    }

    public String uploadPdf(byte[] pdfBytes, String key) {
        try {
            log.info("[S3] Uploading PDF | key={} bytes={}", key, pdfBytes.length);

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType("application/pdf")
                    .contentDisposition("inline; filename=\"" +
                            key.substring(key.lastIndexOf("/") + 1) + "\"")
                    .build();

            s3Client.putObject(request,
                    RequestBody.fromBytes(pdfBytes));

            log.info("[S3] Upload complete | key={}", key);
            return key;

        } catch (Exception ex) {
            log.error("[S3] Upload failed | key={} error={}", key, ex.getMessage());
            throw new RuntimeException("S3 upload failed: " + ex.getMessage());
        }
    }

    public String generatePresignedUrl(String key) {
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(presignedUrlExpiryMinutes))
                    .getObjectRequest(r -> r.bucket(bucket).key(key))
                    .build();

            return presigner.presignGetObject(presignRequest)
                    .url().toString();

        } catch (Exception ex) {
            log.error("[S3] Presign failed | key={} error={}", key, ex.getMessage());
            throw new RuntimeException("S3 presign failed: " + ex.getMessage());
        }
    }

    public void delete(String key) {
        try {
            s3Client.deleteObject(b -> b.bucket(bucket).key(key));
            log.info("[S3] Deleted | key={}", key);
        } catch (Exception ex) {
            log.warn("[S3] Delete failed | key={} error={}", key, ex.getMessage());
        }
    }
}
