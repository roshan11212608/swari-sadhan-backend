package swari.sewa.common.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import swari.sewa.common.exception.StorageException;

import java.util.Set;

/**
 * Centralized validation for image/document uploads.
 *
 * <p>This service is intentionally separate from {@link CloudflareR2StorageService}
 * so validation can be reused and unit-tested independently of object storage.</p>
 */
@Service
@Slf4j
public class ImageValidationService {

    /**
     * Validates a single uploaded file against the rules of the given image type.
     *
     * @param file     the uploaded multipart file
     * @param imageType the image/document type that defines size and MIME-type limits
     * @throws StorageException with a clear, user-friendly message if validation fails
     */
    public void validate(MultipartFile file, ImageType imageType) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("Uploaded file is empty.");
        }

        if (imageType == null) {
            throw new StorageException("Image type is required for upload validation.");
        }

        long maxSize = imageType.getMaxSizeInBytes();
        if (file.getSize() > maxSize) {
            String message = imageType == ImageType.SHOP_LOGO || imageType == ImageType.PROFILE_PHOTO
                    ? String.format("%s size must not exceed %s.",
                        imageType == ImageType.SHOP_LOGO ? "Logo" : "Profile image",
                        imageType.getMaxSizeLabel())
                    : String.format("Image size must not exceed %s.", imageType.getMaxSizeLabel());
            log.warn("Upload rejected: file={} size={} max={} type={}",
                    file.getOriginalFilename(), file.getSize(), maxSize, imageType);
            throw new StorageException(message);
        }

        String contentType = file.getContentType();
        Set<String> allowed = imageType.getAllowedMimeTypes();
        if (contentType == null || !allowed.contains(contentType.toLowerCase())) {
            log.warn("Upload rejected: file={} contentType={} allowed={} type={}",
                    file.getOriginalFilename(), contentType, allowed, imageType);
            throw new StorageException(
                    String.format("Only %s files are allowed for %s uploads.",
                            allowed, imageType.name().toLowerCase().replace('_', ' ')));
        }
    }

    /**
     * Validates each file in an array against the same image type.
     */
    public void validateAll(MultipartFile[] files, ImageType imageType) {
        if (files == null) {
            return;
        }
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                validate(file, imageType);
            }
        }
    }
}
