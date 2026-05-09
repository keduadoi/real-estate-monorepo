package com.realestate.backend.mapper;

import com.realestate.backend.dto.CreatePropertyRequest;
import com.realestate.backend.dto.PropertyDTO;
import com.realestate.backend.dto.UpdatePropertyRequest;
import com.realestate.backend.entity.Property;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Mapper for converting between Property entity and DTOs.
 */
@Component
public class PropertyMapper {

    /**
     * Convert Property entity to PropertyDTO
     */
    public PropertyDTO toDTO(Property property) {
        if (property == null) {
            return null;
        }

        return PropertyDTO.builder()
                .id(property.getId())
                .title(property.getTitle())
                .description(property.getDescription())
                .price(property.getPrice())
                .address(property.getAddress())
                .city(property.getCity())
                .bedrooms(property.getBedrooms())
                .bathrooms(property.getBathrooms())
                .area(property.getArea())
                .propertyType(property.getPropertyType())
                .status(property.getStatus())
                .images(property.getImages() != null ? new ArrayList<>(property.getImages()) : null)
                .features(property.getFeatures() != null ? new ArrayList<>(property.getFeatures()) : null)
                .userId(property.getUserId())
                .createdAt(property.getCreatedAt())
                .updatedAt(property.getUpdatedAt())
                .build();
    }

    /**
     * Convert CreatePropertyRequest to Property entity
     */
    public Property toEntity(CreatePropertyRequest request) {
        if (request == null) {
            return null;
        }

        Property property = new Property();
        property.setTitle(request.getTitle());
        property.setDescription(request.getDescription());
        property.setPrice(request.getPrice());
        property.setAddress(request.getAddress());
        property.setCity(request.getCity());
        property.setBedrooms(request.getBedrooms());
        property.setBathrooms(request.getBathrooms());
        property.setArea(request.getArea());
        property.setPropertyType(request.getPropertyType());
        property.setStatus(request.getStatus());
        property.setImages(request.getImages() != null ? request.getImages() : new ArrayList<>());
        property.setFeatures(request.getFeatures() != null ? request.getFeatures() : new ArrayList<>());
        property.setUserId(request.getUserId());

        return property;
    }

    /**
     * Update Property entity from UpdatePropertyRequest
     * Only updates non-null fields
     */
    public void updateEntity(Property property, UpdatePropertyRequest request) {
        if (request == null || property == null) {
            return;
        }

        if (request.getTitle() != null) {
            property.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            property.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            property.setPrice(request.getPrice());
        }
        if (request.getAddress() != null) {
            property.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            property.setCity(request.getCity());
        }
        if (request.getBedrooms() != null) {
            property.setBedrooms(request.getBedrooms());
        }
        if (request.getBathrooms() != null) {
            property.setBathrooms(request.getBathrooms());
        }
        if (request.getArea() != null) {
            property.setArea(request.getArea());
        }
        if (request.getPropertyType() != null) {
            property.setPropertyType(request.getPropertyType());
        }
        if (request.getStatus() != null) {
            property.setStatus(request.getStatus());
        }
        if (request.getImages() != null) {
            property.setImages(request.getImages());
        }
        if (request.getFeatures() != null) {
            property.setFeatures(request.getFeatures());
        }
    }
}
