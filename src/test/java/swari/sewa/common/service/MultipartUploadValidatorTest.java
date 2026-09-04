package swari.sewa.common.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import swari.sewa.common.exception.StorageException;

import static org.junit.jupiter.api.Assertions.*;

class MultipartUploadValidatorTest {

    private final MultipartUploadValidator validator = new MultipartUploadValidator();

    @Test
    void vehicleMediaFiles_underMaxCount_isAccepted() {
        MockMultipartFile[] files = new MockMultipartFile[10];
        for (int i = 0; i < 10; i++) {
            files[i] = new MockMultipartFile("file", "bike" + i + ".jpg", "image/jpeg", new byte[]{1});
        }

        assertDoesNotThrow(() -> validator.validateFileCount("Vehicle photos", files, 10));
    }

    @Test
    void vehicleMediaFiles_overMaxCount_isRejected() {
        MockMultipartFile[] files = new MockMultipartFile[11];
        for (int i = 0; i < 11; i++) {
            files[i] = new MockMultipartFile("file", "bike" + i + ".jpg", "image/jpeg", new byte[]{1});
        }

        StorageException ex = assertThrows(StorageException.class,
                () -> validator.validateFileCount("Vehicle photos", files, 10));
        assertTrue(ex.getMessage().contains("at most 10"));
    }

    @Test
    void nullArray_isAccepted() {
        assertDoesNotThrow(() -> validator.validateFileCount("Vehicle photos", null, 10));
    }

    @Test
    void emptyFilesAreIgnored() {
        MockMultipartFile[] files = new MockMultipartFile[12];
        files[0] = new MockMultipartFile("file", "bike.jpg", "image/jpeg", new byte[]{1});
        for (int i = 1; i < 12; i++) {
            files[i] = new MockMultipartFile("file", "empty" + i + ".jpg", "image/jpeg", new byte[0]);
        }

        assertDoesNotThrow(() -> validator.validateFileCount("Vehicle photos", files, 10));
    }
}
