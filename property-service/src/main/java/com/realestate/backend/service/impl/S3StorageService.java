package com.realestate.backend.service.impl;

import com.realestate.backend.exception.FileStorageException;
import com.realestate.backend.exception.InvalidFileException;
import com.realestate.backend.service.StorageService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {

    @Value("${storage.s3.bucket-name}")
    private String bucketName;

    @Value("${storage.s3.region:us-east-1}")
    private String region;

    @Value("${storage.s3.access-key}")
    private String accessKey;

    @Value("${storage.s3.secret-key}")
    private String secretKey;

    @Value("${file-upload.allowed-types:image/jpeg,image/png,image/webp}")
    private String allowedTypes;

    @Value("${file-upload.max-file-size:10485760}")
    private long maxFileSize;

    @Value("${file-upload.max-files-per-property:10}")
    private int maxFilesPerProperty;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        log.info("S3 Storage Service initialized with bucket: {}, region: {}", bucketName, region);
    }

    @PreDestroy
    public void cleanup() {
        if (s3Client != null) {
            s3Client.close();
            log.info("S3 client closed");
        }
    }

    @Override
    @CircuitBreaker(name = "s3Storage", fallbackMethod = "storeFileFallback")
    @Retry(name = "s3Storage")
    public String storeFile(Long propertyId, MultipartFile file) {
        validateFile(file);

        try {
            String filename = generateUniqueFilename(file.getOriginalFilename());
            String key = String.format("properties/%d/%s", propertyId, filename);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            String fileUrl = generateS3Url(key);
            log.info("Stored file in S3: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("Failed to store file in S3 for property {}: {}", propertyId, e.getMessage(), e);
            throw new FileStorageException("Failed to store file in S3: " + file.getOriginalFilename(), e);
        } catch (S3Exception e) {
            log.error("S3 error while storing file: {}", e.getMessage(), e);
            throw new FileStorageException("S3 error: " + e.awsErrorDetails().errorMessage(), e);
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
                String key = String.format("temp/%s", filename);

                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(file.getContentType())
                        .acl(ObjectCannedACL.PUBLIC_READ)
                        .build();

                s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

                String fileUrl = generateS3Url(key);
                log.info("Stored temp file in S3: {}", fileUrl);
                urls.add(fileUrl);

            } catch (IOException e) {
                log.error("Failed to store temp file in S3: {}", e.getMessage(), e);
                throw new FileStorageException("Failed to store file in S3: " + file.getOriginalFilename(), e);
            } catch (S3Exception e) {
                log.error("S3 error while storing temp file: {}", e.getMessage(), e);
                throw new FileStorageException("S3 error: " + e.awsErrorDetails().errorMessage(), e);
            }
        }
        return urls;
    }

    @Override
    @CircuitBreaker(name = "s3Storage", fallbackMethod = "deleteFileFallback")
    @Retry(name = "s3Storage")
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            String key = extractKeyFromUrl(fileUrl);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Deleted file from S3: {}", fileUrl);

        } catch (S3Exception e) {
            log.error("S3 error while deleting file {}: {}", fileUrl, e.getMessage(), e);
            throw new FileStorageException("Failed to delete file from S3: " + fileUrl, e);
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

    // Resilience4j fallback methods

    private String storeFileFallback(Long propertyId, MultipartFile file, Throwable t) {
        log.error("S3 circuit breaker open — storeFile fallback for property {}: {}", propertyId, t.getMessage());
        throw new FileStorageException("Storage service is temporarily unavailable. Please try again later.", t);
    }

    private void deleteFileFallback(String fileUrl, Throwable t) {
        log.error("S3 circuit breaker open — deleteFile fallback for {}: {}", fileUrl, t.getMessage());
        throw new FileStorageException("Storage service is temporarily unavailable. Please try again later.", t);
    }

    private String generateUniqueFilename(String originalFilename) {
        String cleanFilename = StringUtils.cleanPath(originalFilename != null ? originalFilename : "file");
        String uuid = UUID.randomUUID().toString();
        return uuid + "_" + cleanFilename;
    }

    private String generateS3Url(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
    }

    private String extractKeyFromUrl(String fileUrl) {
        // Extract key from S3 URL
        // Format: https://bucket.s3.region.amazonaws.com/key
        String prefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
        if (fileUrl.startsWith(prefix)) {
            return fileUrl.substring(prefix.length());
        }
        throw new IllegalArgumentException("Invalid S3 URL: " + fileUrl);
    }
}
