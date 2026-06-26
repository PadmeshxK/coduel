package com.coduel.media;

import com.coduel.common.constant.ApiStatus;
import com.coduel.common.exception.ApiException;
import com.coduel.config.AppProperties;
import com.coduel.interfaces.MediaVault;
import com.coduel.model.constant.Errors;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cloudflare R2 {@link MediaVault} (prod) — pure storage behind the same port as {@link LocalMediaVault}.
 * Uploads the already-validated + downscaled bytes to the bucket over the signed S3 API, and returns an
 * absolute public URL ({publicBaseUrl}/{key}) the browser fetches straight from R2's CDN. Active only
 * when media.storage=r2; otherwise the local-disk vault is used (see @ConditionalOnProperty).
 */
@Component
@ConditionalOnProperty(name = "media.storage", havingValue = "r2")
@Log4j2
public class R2MediaVault implements MediaVault {

    // Content type by extension so the browser serves/plays the object correctly (images + voice audio).
    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("webm", "audio/webm"),
            Map.entry("ogg", "audio/ogg"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("m4a", "audio/mp4"),
            Map.entry("wav", "audio/wav"));

    @Autowired
    private AppProperties appProperties;

    private S3Client s3;
    private String bucket;
    private String publicBaseUrl;

    @PostConstruct
    void init() {
        bucket = appProperties.getR2Bucket();
        publicBaseUrl = appProperties.getR2PublicBaseUrl().replaceAll("/+$", ""); // no trailing slash
        s3 = S3Client.builder()
                .region(Region.of("auto")) // R2 has no regions; "auto" is the convention
                .endpointOverride(URI.create(appProperties.getR2Endpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(appProperties.getR2AccessKey(), appProperties.getR2SecretKey())))
                // path-style (endpoint/bucket/key) — the safe addressing mode against the account endpoint.
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
        log.info("R2 media vault ready: bucket={}, publicBaseUrl={}", bucket, publicBaseUrl);
        // Warm the connection (TLS handshake + pool) off the startup thread, so the FIRST user upload
        // isn't paying cold-start latency. A 403 still warms the socket — we only care about the round-trip.
        Thread warmup = new Thread(() -> {
            try {
                s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            } catch (Exception e) {
                log.debug("R2 warmup probe: {}", e.getMessage());
            }
        }, "r2-warmup");
        warmup.setDaemon(true);
        warmup.start();
    }

    @Override
    public String store(byte[] data, String extension, String folder) throws ApiException {
        // Random UUID key under the folder prefix — unguessable, so the public URL is the capability.
        String key = folder + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String contentType = CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
        try {
            s3.putObject(
                    PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                    RequestBody.fromBytes(data));
        } catch (Exception e) {
            log.error("Failed to store upload in R2: {}", e.getMessage());
            throw new ApiException(ApiStatus.UNKNOWN_ERROR, Errors.ERR_146, List.of());
        }
        return publicBaseUrl + "/" + key;
    }
}
