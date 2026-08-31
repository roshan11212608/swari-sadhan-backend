package swari.sewa.module.fileupload.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import swari.sewa.common.service.StorageCategory;
import swari.sewa.common.service.StorageService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class FileUploadController {

    private final StorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileUrl = storageService.store(file, StorageCategory.MISC, null);
        String filename = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);

        Map<String, String> response = new HashMap<>();
        response.put("url", fileUrl);
        response.put("filename", filename);
        return ResponseEntity.ok(response);
    }
}
