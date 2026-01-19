package com.realestate.backend.controller;

import com.realestate.backend.dto.ImageDeleteRequest;
import com.realestate.backend.dto.ImageUploadResponse;
import com.realestate.backend.service.PropertyService;
import com.realestate.backend.service.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for file upload operations.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {

    private final StorageService storageService;
    private final PropertyService propertyService;

    /**
     * Upload temporary images before property creation
     *
     * @param files The image files to upload
     * @return Response containing uploaded image URLs
     */
    @PostMapping("/upload/temp")
    public ResponseEntity<ImageUploadResponse> uploadTempImages(
            @RequestParam("files") List<MultipartFile> files) {

        log.info("Uploading {} temporary image(s)", files.size());

        List<String> imageUrls = storageService.storeTempFiles(files);

        ImageUploadResponse response = ImageUploadResponse.builder()
                .imageUrls(imageUrls)
                .totalUploaded(imageUrls.size())
                .build();

        log.info("Successfully uploaded {} temporary image(s)", imageUrls.size());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Upload images for an existing property
     *
     * @param id The property ID
     * @param files The image files to upload
     * @return Response containing uploaded image URLs
     */
    @PostMapping("/properties/{id}/images")
    public ResponseEntity<ImageUploadResponse> uploadPropertyImages(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files) {

        log.info("Uploading {} image(s) for property {}", files.size(), id);

        // Verify property exists
        propertyService.findById(id);

        List<String> imageUrls = storageService.storeFiles(id, files);

        ImageUploadResponse response = ImageUploadResponse.builder()
                .imageUrls(imageUrls)
                .totalUploaded(imageUrls.size())
                .build();

        log.info("Successfully uploaded {} image(s) for property {}", imageUrls.size(), id);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Delete images from a property
     *
     * @param id The property ID
     * @param request The request containing image URLs to delete
     * @return Response indicating success
     */
    @DeleteMapping("/properties/{id}/images")
    public ResponseEntity<Void> deletePropertyImages(
            @PathVariable Long id,
            @Valid @RequestBody ImageDeleteRequest request) {

        log.info("Deleting {} image(s) from property {}", request.getImageUrls().size(), id);

        // Verify property exists
        propertyService.findById(id);

        storageService.deleteFiles(request.getImageUrls());

        log.info("Successfully deleted {} image(s) from property {}", request.getImageUrls().size(), id);
        return ResponseEntity.noContent().build();
    }
}
