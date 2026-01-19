package com.realestate.backend.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StorageService {

    /**
     * Store a single file for a property
     *
     * @param propertyId The ID of the property
     * @param file The file to store
     * @return The URL where the file can be accessed
     */
    String storeFile(Long propertyId, MultipartFile file);

    /**
     * Store multiple files for a property
     *
     * @param propertyId The ID of the property
     * @param files The files to store
     * @return List of URLs where the files can be accessed
     */
    List<String> storeFiles(Long propertyId, List<MultipartFile> files);

    /**
     * Store temporary files before property creation
     *
     * @param files The files to store
     * @return List of URLs where the files can be accessed
     */
    List<String> storeTempFiles(List<MultipartFile> files);

    /**
     * Delete a single file
     *
     * @param fileUrl The URL of the file to delete
     */
    void deleteFile(String fileUrl);

    /**
     * Delete multiple files
     *
     * @param fileUrls The URLs of the files to delete
     */
    void deleteFiles(List<String> fileUrls);

    /**
     * Validate a file before upload
     *
     * @param file The file to validate
     * @throws com.realestate.backend.exception.InvalidFileException if validation fails
     */
    void validateFile(MultipartFile file);
}
