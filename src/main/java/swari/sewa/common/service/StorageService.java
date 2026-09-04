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
    default String store(MultipartFile file, StorageCategory category, Long entityId) {
        return store(file, category, entityId, ImageType.fromCategory(category));
    }

    /**
     * Uploads a single file with an explicit image/document type and returns its public URL.
     *
     * <p>The image type controls per-file size and MIME-type limits. Use this overload
     * when a storage category contains files with different limits (e.g. a {@code SHOP}
     * category that contains both 1 MB logos and 2 MB banners).</p>
     *
     * @param file      the uploaded multipart file
     * @param category  object key prefix category
     * @param entityId  parent entity id, or {@code null}
     * @param imageType the image/document type that defines size/MIME limits
     */
    String store(MultipartFile file, StorageCategory category, Long entityId, ImageType imageType);

    /**
     * Uploads multiple files and returns their public URLs in order.
     */
    default List<String> storeAll(MultipartFile[] files, StorageCategory category, Long entityId) {
        return storeAll(files, category, entityId, ImageType.fromCategory(category));
    }

    /**
     * Uploads multiple files with an explicit image/document type.
     */
    List<String> storeAll(MultipartFile[] files, StorageCategory category, Long entityId, ImageType imageType);

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
