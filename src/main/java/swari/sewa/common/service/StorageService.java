package swari.sewa.common.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Abstraction over object storage (currently Cloudflare R2).
 */
public interface StorageService {

    /**
     * Uploads a single file and returns its public URL.
     *
     * @param file      the uploaded multipart file
     * @param category  object key prefix category (e.g. {@code vehicles})
     * @param entityId  parent entity id, or {@code null} for a not-yet-persisted entity
     */
    String store(MultipartFile file, StorageCategory category, Long entityId);

    /**
     * Uploads multiple files and returns their public URLs in order.
     */
    List<String> storeAll(MultipartFile[] files, StorageCategory category, Long entityId);

    /**
     * Deletes the object referenced by a previously returned public URL.
     * Non-R2 URLs (e.g. legacy {@code /uploads/...}) are ignored.
     *
     * @return {@code true} if an object was deleted, {@code false} otherwise
     */
    boolean deleteByUrl(String publicUrl);

    /**
     * Builds the public URL for the given object key.
     */
    String toPublicUrl(String objectKey);
}
