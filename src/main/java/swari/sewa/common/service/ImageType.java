package swari.sewa.common.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Image/document upload types with per-type size limits and allowed MIME types.
 *
 * <p>These types are independent of {@link StorageCategory} (which only controls
 * the R2 object-key prefix). A single category can contain multiple image types
 * with different limits, e.g. {@code SHOP} contains both {@link #SHOP_LOGO} (1 MB)
 * and {@link #SHOP_BANNER} (2 MB).</p>
 */
@Getter
@RequiredArgsConstructor
public enum ImageType {

    VEHICLE_PHOTO(2L * 1024 * 1024, ImageMimeTypes.IMAGES),
    VEHICLE_BLUEBOOK(2L * 1024 * 1024, ImageMimeTypes.IMAGES),
    VEHICLE_DOCUMENT(2L * 1024 * 1024, ImageMimeTypes.IMAGES),

    SHOP_LOGO(1L * 1024 * 1024, ImageMimeTypes.IMAGES),
    SHOP_BANNER(2L * 1024 * 1024, ImageMimeTypes.IMAGES),
    SHOP_IMAGE(2L * 1024 * 1024, ImageMimeTypes.IMAGES),
    SHOP_REGISTRATION_DOC(2L * 1024 * 1024, ImageMimeTypes.IMAGES_AND_PDF),

    PROFILE_PHOTO(1L * 1024 * 1024, ImageMimeTypes.IMAGES),
    USER_DOCUMENT(2L * 1024 * 1024, ImageMimeTypes.IMAGES_AND_PDF),

    EXPENSE_ATTACHMENT(2L * 1024 * 1024, ImageMimeTypes.IMAGES_AND_PDF),

    MISC(2L * 1024 * 1024, ImageMimeTypes.IMAGES_AND_PDF);

    private final long maxSizeInBytes;
    private final Set<String> allowedMimeTypes;

    /**
     * Human-readable size label (e.g. "2 MB").
     */
    public String getMaxSizeLabel() {
        if (maxSizeInBytes >= 1024 * 1024) {
            return (maxSizeInBytes / (1024 * 1024)) + " MB";
        }
        return (maxSizeInBytes / 1024) + " KB";
    }

    /**
     * Returns the fallback {@link ImageType} for a {@link StorageCategory} when
     * the caller does not specify an explicit image type. This keeps existing
     * 3-arg {@code StorageService.store(...)} calls working.
     */
    public static ImageType fromCategory(StorageCategory category) {
        return switch (category) {
            case VEHICLE -> VEHICLE_PHOTO;
            case SHOP -> SHOP_IMAGE;
            case USER -> USER_DOCUMENT;
            case SHOP_REGISTRATION -> SHOP_REGISTRATION_DOC;
            case EXPENSE -> EXPENSE_ATTACHMENT;
            case MISC -> MISC;
        };
    }

    private static final class ImageMimeTypes {
        static final Set<String> IMAGES = Set.of(
                "image/jpeg",
                "image/jpg",
                "image/png",
                "image/webp"
        );

        static final Set<String> IMAGES_AND_PDF = Set.of(
                "image/jpeg",
                "image/jpg",
                "image/png",
                "image/webp",
                "application/pdf"
        );
    }
}
