-- Add index on property_images.image_url for faster lookups
CREATE INDEX IF NOT EXISTS idx_property_images_url ON property_images(image_url);

-- Add comment to document the limit
COMMENT ON TABLE property_images IS 'Images associated with properties. Maximum 10 images per property (enforced at application level).';

-- Add a constraint function to enforce max 10 images per property
CREATE OR REPLACE FUNCTION check_max_images_per_property()
RETURNS TRIGGER AS $$
DECLARE
    image_count INTEGER;
BEGIN
    -- Count existing images for this property
    SELECT COUNT(*) INTO image_count
    FROM property_images
    WHERE property_id = NEW.property_id;

    -- If inserting and already at limit, reject
    IF TG_OP = 'INSERT' AND image_count >= 10 THEN
        RAISE EXCEPTION 'Cannot add more than 10 images per property';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to enforce max images constraint
DROP TRIGGER IF EXISTS trigger_check_max_images_per_property ON property_images;
CREATE TRIGGER trigger_check_max_images_per_property
    BEFORE INSERT ON property_images
    FOR EACH ROW
    EXECUTE FUNCTION check_max_images_per_property();
