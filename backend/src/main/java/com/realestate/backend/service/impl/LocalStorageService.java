package com.realestate.backend.service.impl;

import com.realestate.backend.exception.FileStorageException;
import com.realestate.backend.exception.InvalidFileException;
import com.realestate.backend.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    @Value("${storage.local.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${storage.local.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${file-upload.allowed-types:image/jpeg,image/png,image/webp}")
    private String allowedTypes;

    @Value("${file-upload.max-file-size:10485760}")
    private long maxFileSize; // 10MB in bytes

    @Value("${file-upload.max-files-per-property:10}")
    private int maxFilesPerProperty;

    @Override
    public String storeFile(Long propertyId, MultipartFile file) {
        validateFile(file);

        try {
            String filename = generateUniqueFilename(file.getOriginalFilename());
            Path propertyDir = getPropertyDirectory(propertyId);
            Path targetLocation = propertyDir.resolve(filename);

            Files.createDirectories(propertyDir);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            String fileUrl = String.format("%s/uploads/properties/%d/%s", baseUrl, propertyId, filename);
            log.info("Stored file: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("Failed to store file for property {}: {}", propertyId, e.getMessage(), e);
            throw new FileStorageException("Failed to store file: " + file.getOriginalFilename(), e);
        }
    }

    @Override
    public List<String> storeFiles(Long propertyId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        if (files.size() > maxFilesPerProperty) {
            throw new InvalidFileException(
                String.format("Cannot upload more than %d files per property", maxFilesPerProperty)
            );
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(storeFile(propertyId, file));
        }
        return urls;
    }

    @Override
    public List<String> storeTempFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        if (files.size() > maxFilesPerProperty) {
            throw new InvalidFileException(
                String.format("Cannot upload more than %d files", maxFilesPerProperty)
            );
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            validateFile(file);

            try {
                String filename = generateUniqueFilename(file.getOriginalFilename());
                Path tempDir = getTempDirectory();
                Path targetLocation = tempDir.resolve(filename);

                Files.createDirectories(tempDir);

                try (InputStream inputStream = file.getInputStream()) {
                    Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
                }

                String fileUrl = String.format("%s/uploads/temp/%s", baseUrl, filename);
                log.info("Stored temp file: {}", fileUrl);
                urls.add(fileUrl);

            } catch (IOException e) {
                log.error("Failed to store temp file: {}", e.getMessage(), e);
                throw new FileStorageException("Failed to store file: " + file.getOriginalFilename(), e);
            }
        }
        return urls;
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            Path filePath = extractFilePathFromUrl(fileUrl);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted file: {}", fileUrl);
            } else {
                log.warn("File not found for deletion: {}", fileUrl);
            }
        } catch (IOException e) {
            log.error("Failed to delete file {}: {}", fileUrl, e.getMessage(), e);
            throw new FileStorageException("Failed to delete file: " + fileUrl, e);
        }
    }

    @Override
    public void deleteFiles(List<String> fileUrls) {
        if (fileUrls == null || fileUrls.isEmpty()) {
            return;
        }

        for (String url : fileUrls) {
            deleteFile(url);
        }
    }

    @Override
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File cannot be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !Arrays.asList(allowedTypes.split(",")).contains(contentType)) {
            throw new InvalidFileException(
                String.format("Invalid file type: %s. Allowed types: %s", contentType, allowedTypes)
            );
        }

        if (file.getSize() > maxFileSize) {
            throw new InvalidFileException(
                String.format("File size exceeds maximum allowed size of %d MB", maxFileSize / (1024 * 1024))
            );
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.contains("..")) {
            throw new InvalidFileException("Invalid filename: " + filename);
        }
    }

    private String generateUniqueFilename(String originalFilename) {
        String cleanFilename = StringUtils.cleanPath(originalFilename != null ? originalFilename : "file");
        String uuid = UUID.randomUUID().toString();
        return uuid + "_" + cleanFilename;
    }

    private Path getPropertyDirectory(Long propertyId) {
        return Paths.get(uploadDir, "properties", propertyId.toString());
    }

    private Path getTempDirectory() {
        return Paths.get(uploadDir, "temp");
    }

    private Path extractFilePathFromUrl(String fileUrl) {
        String path = fileUrl.replace(baseUrl, "");
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return Paths.get(path);
    }
}
