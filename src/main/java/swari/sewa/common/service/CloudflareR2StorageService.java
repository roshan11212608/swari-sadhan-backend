package swari.sewa.common.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import swari.sewa.common.exception.StorageException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Cloudflare R2 (S3-compatible) implementation of {@link StorageService}.
 */
@Service
@Slf4j
public class CloudflareR2StorageService implements StorageService {

    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L; // 10 MB

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
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint.trim()))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey == null ? "" : accessKey.trim(),
                                secretKey == null ? "" : secretKey.trim())))
                .build();
        log.info("Cloudflare R2 storage initialized for bucket '{}' with endpoint '{}'", bucketName, endpoint);
    }

    @Override
    public String store(MultipartFile file, StorageCategory category, Long entityId) {
        if (s3Client == null) {
            throw new StorageException("Object storage is not configured. Please set the R2 environment variables.");
        }
        validate(file);

        String objectKey = buildObjectKey(file, category, entityId);

        try (InputStream input = file.getInputStream()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(input, file.getSize()));
        } catch (IOException e) {
            throw new StorageException("Failed to read uploaded file", e);
        } catch (Exception e) {
            throw new StorageException("Failed to upload file to object storage", e);
        }

        return toPublicUrl(objectKey);
    }

    @Override
    public List<String> storeAll(MultipartFile[] files, StorageCategory category, Long entityId) {
        List<String> urls = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    urls.add(store(file, category, entityId));
                }
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

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());
            log.info("Deleted R2 object '{}'", objectKey);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete R2 object '{}': {}", objectKey, e.getMessage());
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

    private String buildObjectKey(MultipartFile file, StorageCategory category, Long entityId) {
        String extension = extractExtension(file.getOriginalFilename());
        String unique = UUID.randomUUID().toString() + extension;

        if (category == StorageCategory.MISC) {
            return category.getPrefix() + "/" + unique;
        }
        String idPart = entityId != null ? entityId.toString() : "new";
        return category.getPrefix() + "/" + idPart + "/" + unique;
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("Uploaded file is empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new StorageException("File size exceeds the 10MB limit.");
        }
        String contentType = file.getContentType();
        if (contentType == null
                || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
            throw new StorageException("Only image and PDF files are allowed.");
        }
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
