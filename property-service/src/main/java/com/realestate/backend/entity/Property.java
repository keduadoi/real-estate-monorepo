package com.realestate.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Property entity representing real estate listings.
 * Matches the UI Property interface structure.
 */
@Entity
@Table(name = "properties")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Description is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @NotBlank(message = "Address is required")
    @Column(nullable = false)
    private String address;

    @NotBlank(message = "City is required")
    @Column(nullable = false)
    private String city;

    @NotNull(message = "Number of bedrooms is required")
    @PositiveOrZero(message = "Bedrooms must be zero or positive")
    @Column(nullable = false)
    private Integer bedrooms;

    @NotNull(message = "Number of bathrooms is required")
    @PositiveOrZero(message = "Bathrooms must be zero or positive")
    @Column(nullable = false)
    private Integer bathrooms;

    @NotNull(message = "Area is required")
    @Positive(message = "Area must be positive")
    @Column(nullable = false)
    private Integer area;

    @NotNull(message = "Property type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropertyType propertyType;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropertyStatus status;

    /**
     * List of image URLs for the property.
     * Stored as a separate collection table.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "property_images", joinColumns = @JoinColumn(name = "property_id"))
    @Column(name = "image_url")
    private List<String> images = new ArrayList<>();

    /**
     * List of property features (e.g., "Bãi đậu xe", "Sân vườn", "Hồ bơi").
     * Stored as a separate collection table.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "property_features", joinColumns = @JoinColumn(name = "property_id"))
    @Column(name = "feature")
    private List<String> features = new ArrayList<>();

    /**
     * User ID of the property owner/creator.
     * This will be a foreign key reference to the User entity when implemented.
     */
    @NotBlank(message = "User ID is required")
    @Column(nullable = false)
    private String userId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Geographic coordinates derived from {@link #address} + {@link #city}.
     * Null until the geocoding pipeline has produced a SUCCESS result.
     */
    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "geocoded_at")
    private LocalDateTime geocodedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "geocoding_status", nullable = false, length = 20)
    private GeocodingStatus geocodingStatus = GeocodingStatus.PENDING;
}
