package swari.sewa.common.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import swari.sewa.common.exception.StorageException;

import static org.junit.jupiter.api.Assertions.*;

class ImageValidationServiceTest {

    private final ImageValidationService service = new ImageValidationService();

    @Test
    void vehiclePhoto_500KB_jpeg_isAccepted() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "vehicle.jpg", "image/jpeg", new byte[500 * 1024]);

        assertDoesNotThrow(() -> service.validate(file, ImageType.VEHICLE_PHOTO));
    }

    @Test
    void vehiclePhoto_1MB_jpeg_isAccepted() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "vehicle.jpg", "image/jpeg", new byte[1024 * 1024]);

        assertDoesNotThrow(() -> service.validate(file, ImageType.VEHICLE_PHOTO));
    }

    @Test
    void vehiclePhoto_1900KB_jpeg_isAccepted() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "vehicle.jpg", "image/jpeg", new byte[1900 * 1024]);

        assertDoesNotThrow(() -> service.validate(file, ImageType.VEHICLE_PHOTO));
    }

    @Test
    void vehiclePhoto_exactly2MB_jpeg_isAccepted() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "vehicle.jpg", "image/jpeg", new byte[2 * 1024 * 1024]);

        assertDoesNotThrow(() -> service.validate(file, ImageType.VEHICLE_PHOTO));
    }

    @Test
    void vehiclePhoto_over2MB_jpeg_isRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "vehicle.jpg", "image/jpeg", new byte[2 * 1024 * 1024 + 1]);

        StorageException ex = assertThrows(StorageException.class,
                () -> service.validate(file, ImageType.VEHICLE_PHOTO));
        assertTrue(ex.getMessage().contains("2 MB"));
    }

    @Test
    void shopLogo_over1MB_jpeg_isRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "logo.jpg", "image/jpeg", new byte[1 * 1024 * 1024 + 1]);

        StorageException ex = assertThrows(StorageException.class,
                () -> service.validate(file, ImageType.SHOP_LOGO));
        assertTrue(ex.getMessage().contains("1 MB"));
    }

    @Test
    void profilePhoto_exactly1MB_jpeg_isAccepted() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "profile.jpg", "image/jpeg", new byte[1 * 1024 * 1024]);

        assertDoesNotThrow(() -> service.validate(file, ImageType.PROFILE_PHOTO));
    }

    @Test
    void unsupportedFileType_isRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.sh", "application/x-sh", new byte[1024]);

        StorageException ex = assertThrows(StorageException.class,
                () -> service.validate(file, ImageType.VEHICLE_PHOTO));
        assertTrue(ex.getMessage().contains("Only"));
    }

    @Test
    void webpVehiclePhoto_isAccepted() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "vehicle.webp", "image/webp", new byte[1024]);

        assertDoesNotThrow(() -> service.validate(file, ImageType.VEHICLE_PHOTO));
    }

    @Test
    void pngVehiclePhoto_isAccepted() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "vehicle.png", "image/png", new byte[1024]);

        assertDoesNotThrow(() -> service.validate(file, ImageType.VEHICLE_PHOTO));
    }

    @Test
    void expenseAttachment_pdf_under2MB_isAccepted() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "receipt.pdf", "application/pdf", new byte[1024 * 1024]);

        assertDoesNotThrow(() -> service.validate(file, ImageType.EXPENSE_ATTACHMENT));
    }

    @Test
    void emptyFile_isRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        StorageException ex = assertThrows(StorageException.class,
                () -> service.validate(file, ImageType.VEHICLE_PHOTO));
        assertTrue(ex.getMessage().contains("empty"));
    }
}
