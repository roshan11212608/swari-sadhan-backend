package swari.sewa.common.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import swari.sewa.common.exception.StorageException;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Cloudflare R2 (S3-compatible) implementation of {@link StorageService}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CloudflareR2StorageService implements StorageService {

    private final ImageValidationService imageValidationService;

    @Value("${r2.endpoint:}")
    private String endpoint;

    @Value("${r2.access-key:}")
    private String accessKey;

    @Value("${r2.secret-key:}")
    private String secretKey;

    @Value("${r2.bucket-name:swari-sadhan-images}")
    private String bucketName;

    @Value("${r2.public-url:}")
    private String publicUrl;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        if (endpoint == null || endpoint.isBlank()) {
            log.warn("R2 endpoint is not configured; R2 storage is unavailable until R2_* environment variables are set.");
            return;
        }

        SdkHttpClient httpClient = UrlConnectionHttpClient.builder()
                .connectionTimeout(Duration.ofSeconds(10))
                .socketTimeout(Duration.ofSeconds(30))
                .build();

        ClientOverrideConfiguration overrideConfiguration = ClientOverrideConfiguration.builder()
                .apiCallTimeout(Duration.ofSeconds(60))
                .apiCallAttemptTimeout(Duration.ofSeconds(45))
                .build();

        // R2 requires path-style access and non-chunked encoding for correct
        // request signing (chunked encoding causes HTTP 403 signature mismatches).
        S3Configuration s3Configuration = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .chunkedEncodingEnabled(false)
                .build();

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint.trim()))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey == null ? "" : accessKey.trim(),
                                secretKey == null ? "" : secretKey.trim())))
                .httpClient(httpClient)
                .overrideConfiguration(overrideConfiguration)
                .serviceConfiguration(s3Configuration)
                .build();
        log.info("Cloudflare R2 storage initialized for bucket '{}' with endpoint '{}'", bucketName, endpoint);
    }

    @Override
    public String store(MultipartFile file, StorageCategory category, Long entityId, ImageType imageType) {
        if (s3Client == null) {
            throw new StorageException("Object storage is not configured. Please set the R2 environment variables.");
        }

        if (imageType == null) {
            imageType = ImageType.fromCategory(category);
        }

        imageValidationService.validate(file, imageType);

        String objectKey = buildObjectKey(file, category, entityId);

        long start = System.nanoTime();
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();
            // Load into memory (max now controlled by per-image validation) instead of streaming,
            // because the SDK's fromInputStream path requires a mark/reset-capable stream and throws
            // IllegalStateException on retry for standard servlet MultipartFile streams.
            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            log.info("R2 upload succeeded: operation=putObject bucket={} key={} size={} type={} elapsedMs={}",
                    bucketName, objectKey, file.getSize(), imageType, elapsedMs);
        } catch (IOException e) {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            log.error("R2 upload read failed: operation=putObject bucket={} key={} elapsedMs={} errorType={}",
                    bucketName, objectKey, elapsedMs, e.getClass().getSimpleName());
            throw new StorageException("Failed to read uploaded file", e);
        } catch (Exception e) {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            log.error("R2 upload failed: operation=putObject bucket={} key={} elapsedMs={} errorType={}",
                    bucketName, objectKey, elapsedMs, e.getClass().getSimpleName());
            throw new StorageException("Failed to upload file to object storage", e);
        }

        return toPublicUrl(objectKey);
    }

    @Override
    public List<String> storeAll(MultipartFile[] files, StorageCategory category, Long entityId, ImageType imageType) {
        List<String> urls = new ArrayList<>();
        if (files == null) {
            return urls;
        }

        // Determine effective image type and validate every file before uploading any.
        // This prevents partial uploads when a batch contains an oversized/invalid file.
        ImageType effectiveType = imageType != null ? imageType : ImageType.fromCategory(category);
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                imageValidationService.validate(file, effectiveType);
            }
        }

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                urls.add(store(file, category, entityId, effectiveType));
            }
        }
        return urls;
    }

    @Override
    public boolean deleteByUrl(String publicUrl) {
        if (s3Client == null || publicUrl == null || publicUrl.isBlank()) {
            return false;
        }

        String objectKey = extractObjectKey(publicUrl);
        if (objectKey == null) {
            log.debug("Skipping delete for non-R2 URL: {}", publicUrl);
            return false;
        }

        long start = System.nanoTime();
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            log.info("R2 delete succeeded: operation=deleteObject bucket={} key={} elapsedMs={}",
                    bucketName, objectKey, elapsedMs);
            return true;
        } catch (Exception e) {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            log.error("R2 delete failed: operation=deleteObject bucket={} key={} elapsedMs={} errorType={}",
                    bucketName, objectKey, elapsedMs, e.getClass().getSimpleName());
            return false;
        }
    }

    @Override
    public String toPublicUrl(String objectKey) {
        String base = publicUrl == null ? "" : publicUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + objectKey;
    }

    /**
     * Lightweight connectivity diagnostic. Does not upload or expose credentials.
     *
     * @return {@code true} when the R2 bucket is reachable with the configured credentials
     */
    public boolean checkConnectivity() {
        if (s3Client == null) {
            log.warn("R2 connectivity check skipped: storage is not configured.");
            return false;
        }

        long start = System.nanoTime();
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            log.info("R2 connectivity check succeeded: operation=headBucket bucket={} elapsedMs={}",
                    bucketName, elapsedMs);
            return true;
        } catch (Exception e) {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            log.error("R2 connectivity check failed: operation=headBucket bucket={} elapsedMs={} errorType={}",
                    bucketName, elapsedMs, e.getClass().getSimpleName());
            return false;
        }
    }

    private String buildObjectKey(MultipartFile file, StorageCategory category, Long entityId) {
        String extension = extractExtension(file.getOriginalFilename());
        String unique = UUID.randomUUID().toString() + extension;

        if (category == StorageCategory.MISC) {
            return category.getPrefix() + "/" + unique;
        }
        String idPart = entityId != null ? entityId.toString() : "new";
        return category.getPrefix() + "/" + idPart + "/" + unique;
    }

    private String extractObjectKey(String publicUrl) {
        String base = this.publicUrl == null ? "" : this.publicUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isBlank()) {
            return null;
        }
        String prefix = base + "/";
        if (publicUrl.startsWith(prefix)) {
            return publicUrl.substring(prefix.length());
        }
        return null;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        // Keep the extension safe: only allow simple alphanumeric extensions up to 10 chars.
        if (ext.length() > 10 || !ext.substring(1).matches("[a-z0-9]+")) {
            return "";
        }
        return ext;
    }
}
