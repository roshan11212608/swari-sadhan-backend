package swari.sewa.common.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import swari.sewa.common.exception.StorageException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CloudflareR2StorageServiceTest {

    private static final String BUCKET = "swari-sadhan-images";
    private static final String PUBLIC_URL = "https://images.swarisadhan.com";

    private CloudflareR2StorageService service;
    private S3Client s3Client;

    @BeforeEach
    void setUp() {
        service = new CloudflareR2StorageService();
        s3Client = mock(S3Client.class);

        ReflectionTestUtils.setField(service, "s3Client", s3Client);
        ReflectionTestUtils.setField(service, "bucketName", BUCKET);
        ReflectionTestUtils.setField(service, "publicUrl", PUBLIC_URL);
    }

    @Test
    void store_success_returnsPublicUrlAndUsesVehicleKey() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "bike.jpg", "image/jpeg", new byte[]{1, 2, 3});

        String url = service.store(file, StorageCategory.VEHICLE, 101L);

        assertTrue(url.startsWith(PUBLIC_URL + "/vehicles/101/"));
        assertTrue(url.endsWith(".jpg"));
    }

    @Test
    void store_success_usesNewWhenEntityIdIsNull() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[]{1});

        String url = service.store(file, StorageCategory.USER, null);

        assertTrue(url.startsWith(PUBLIC_URL + "/users/new/"));
    }

    @Test
    void store_invalidContentType_throwsStorageException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.sh", "text/plain", new byte[]{1});

        assertThrows(StorageException.class,
                () -> service.store(file, StorageCategory.MISC, null));
        verifyNoInteractions(s3Client);
    }

    @Test
    void store_oversizedFile_throwsStorageException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.jpg", "image/jpeg", new byte[11 * 1024 * 1024]);

        assertThrows(StorageException.class,
                () -> service.store(file, StorageCategory.VEHICLE, 1L));
        verifyNoInteractions(s3Client);
    }

    @Test
    void store_notConfigured_throwsStorageException() {
        ReflectionTestUtils.setField(service, "s3Client", null);
        MockMultipartFile file = new MockMultipartFile(
                "file", "bike.jpg", "image/jpeg", new byte[]{1});

        assertThrows(StorageException.class,
                () -> service.store(file, StorageCategory.VEHICLE, 1L));
    }

    @Test
    void deleteByUrl_deletesObjectForKey() {
        boolean deleted = service.deleteByUrl(PUBLIC_URL + "/vehicles/101/uuid.jpg");

        assertTrue(deleted);
    }

    @Test
    void deleteByUrl_ignoresNonR2Url() {
        boolean deleted = service.deleteByUrl("http://localhost:8081/uploads/uuid.jpg");

        assertFalse(deleted);
        verifyNoInteractions(s3Client);
    }
}
