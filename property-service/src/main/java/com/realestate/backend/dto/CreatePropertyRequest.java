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
 * Request DTO for creating a new property.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePropertyRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 10, max = 255, message = "Title must be between 10 and 255 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 20, max = 5000, message = "Description must be between 20 and 5000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 255, message = "City must not exceed 255 characters")
    private String city;

    @NotNull(message = "Number of bedrooms is required")
    @Min(value = 0, message = "Bedrooms must be 0 or greater")
    @Max(value = 50, message = "Bedrooms must not exceed 50")
    private Integer bedrooms;

    @NotNull(message = "Number of bathrooms is required")
    @Min(value = 0, message = "Bathrooms must be 0 or greater")
    @Max(value = 50, message = "Bathrooms must not exceed 50")
    private Integer bathrooms;

    @NotNull(message = "Area is required")
    @Min(value = 1, message = "Area must be at least 1 square meter")
    @Max(value = 100000, message = "Area must not exceed 100000 square meters")
    private Integer area;

    @NotNull(message = "Property type is required")
    private PropertyType propertyType;

    @NotNull(message = "Status is required")
    private PropertyStatus status;

    private List<String> images;

    private List<String> features;

    /**
     * User ID is optional - if not provided, it will be automatically set from
     * the authenticated user's JWT token via UserContext.
     */
    private String userId;
}
