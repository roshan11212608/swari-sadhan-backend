package swari.sewa.common.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import swari.sewa.common.exception.StorageException;

/**
 * Validates multipart upload counts and size/type limits for the application.
 *
 * <p>All byte limits are still enforced by {@link ImageValidationService} inside
 * {@link CloudflareR2StorageService}; this validator is responsible for request-level
 * limits such as the maximum number of files accepted by each endpoint.</p>
 */
@Service
public class MultipartUploadValidator {

    public static final int MAX_VEHICLE_MEDIA_FILES = 10;
    public static final int MAX_VEHICLE_BLUEBOOK_FILES = 3;

    public static final int MAX_PUBLIC_VEHICLE_PHOTOS = 10;
    public static final int MAX_PUBLIC_BLUEBOOK_FILES = 3;
    public static final int MAX_PUBLIC_CITIZENSHIP_FILES = 1;

    public static final int MAX_SELL_APPLICATION_CUSTOMER_PHOTOS = 1;
    public static final int MAX_SELL_APPLICATION_CITIZENSHIP_FRONT = 1;
    public static final int MAX_SELL_APPLICATION_CITIZENSHIP_BACK = 1;

    public static final int MAX_SHOP_REGISTRATION_FILES = 5;
    public static final int MAX_PROFILE_PHOTO_FILES = 1;
    public static final int MAX_MISCELLANEOUS_FILES = 1;
    public static final int MAX_EXPENSE_ATTACHMENTS = 1;

    public void validateFileCount(String fieldName, MultipartFile[] files, int maxFiles) {
        if (files == null) {
            return;
        }
        long nonEmptyCount = java.util.Arrays.stream(files)
                .filter(f -> f != null && !f.isEmpty())
                .count();
        if (nonEmptyCount > maxFiles) {
            throw new StorageException(
                    String.format("%s accepts at most %d file(s). Received %d.", fieldName, maxFiles, nonEmptyCount));
        }
    }
}
