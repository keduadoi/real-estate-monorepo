package com.realestate.backend.dto;

import com.realestate.backend.entity.GeocodingStatus;
import com.realestate.backend.entity.PropertyStatus;
import com.realestate.backend.entity.PropertyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for Property entity.
 * Used for API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyDTO {

    private Long id;
    private String title;
    private String description;
    private BigDecimal price;
    private String address;
    private String city;
    private Integer bedrooms;
    private Integer bathrooms;
    private Integer area;
    private PropertyType propertyType;
    private PropertyStatus status;
    private List<String> images;
    private List<String> features;
    private String userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double latitude;
    private Double longitude;
    private GeocodingStatus geocodingStatus;
}
