-- Create properties table
CREATE TABLE properties (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    price NUMERIC(19, 2) NOT NULL CHECK (price > 0),
    address VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    bedrooms INTEGER NOT NULL CHECK (bedrooms >= 0),
    bathrooms INTEGER NOT NULL CHECK (bathrooms >= 0),
    area INTEGER NOT NULL CHECK (area > 0),
    property_type VARCHAR(50) NOT NULL CHECK (property_type IN ('HOUSE', 'APARTMENT', 'VILLA', 'TOWNHOUSE')),
    status VARCHAR(50) NOT NULL CHECK (status IN ('FOR_SALE', 'FOR_RENT')),
    user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create property_images table for ElementCollection
CREATE TABLE property_images (
    property_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    CONSTRAINT fk_property_images_property FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE
);

-- Create property_features table for ElementCollection
CREATE TABLE property_features (
    property_id BIGINT NOT NULL,
    feature VARCHAR(255) NOT NULL,
    CONSTRAINT fk_property_features_property FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_properties_city ON properties(city);
CREATE INDEX idx_properties_property_type ON properties(property_type);
CREATE INDEX idx_properties_status ON properties(status);
CREATE INDEX idx_properties_user_id ON properties(user_id);
CREATE INDEX idx_properties_created_at ON properties(created_at DESC);
CREATE INDEX idx_properties_price ON properties(price);
CREATE INDEX idx_property_images_property_id ON property_images(property_id);
CREATE INDEX idx_property_features_property_id ON property_features(property_id);

-- Add comments for documentation
COMMENT ON TABLE properties IS 'Real estate property listings';
COMMENT ON TABLE property_images IS 'Images associated with properties';
COMMENT ON TABLE property_features IS 'Features/amenities of properties';
COMMENT ON COLUMN properties.price IS 'Price in VND for sale properties or monthly rent';
COMMENT ON COLUMN properties.area IS 'Property area in square meters';
