package com.realestate.backend.dto;

import com.realestate.backend.entity.PropertyStatus;
import com.realestate.backend.entity.PropertyType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for updating an existing property.
 * All fields are optional to support partial updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePropertyRequest {

    @Size(min = 10, max = 255, message = "Title must be between 10 and 255 characters")
    private String title;

    @Size(min = 20, max = 5000, message = "Description must be between 20 and 5000 characters")
    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @Size(max = 255, message = "City must not exceed 255 characters")
    private String city;

    @Min(value = 0, message = "Bedrooms must be 0 or greater")
    @Max(value = 50, message = "Bedrooms must not exceed 50")
    private Integer bedrooms;

    @Min(value = 0, message = "Bathrooms must be 0 or greater")
    @Max(value = 50, message = "Bathrooms must not exceed 50")
    private Integer bathrooms;

    @Min(value = 1, message = "Area must be at least 1 square meter")
    @Max(value = 100000, message = "Area must not exceed 100000 square meters")
    private Integer area;

    private PropertyType propertyType;

    private PropertyStatus status;

    private List<String> images;

    private List<String> features;
}
