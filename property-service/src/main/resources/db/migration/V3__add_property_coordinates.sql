-- Add geographic coordinates and geocoding lifecycle columns to properties.
-- Coordinates are nullable: NULL until the address has been successfully geocoded.
-- geocoding_status drives the backfill runner and async create/update geocoding hook.

ALTER TABLE properties
    ADD COLUMN latitude         DOUBLE PRECISION,
    ADD COLUMN longitude        DOUBLE PRECISION,
    ADD COLUMN geocoded_at      TIMESTAMP,
    ADD COLUMN geocoding_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (geocoding_status IN ('PENDING', 'SUCCESS', 'FAILED', 'SKIPPED'));

-- Index for the backfill runner: it scans for rows that need (re)geocoding.
CREATE INDEX idx_properties_geocoding_status ON properties (geocoding_status);

-- Partial index for future bounding-box queries; only covers geocoded rows.
CREATE INDEX idx_properties_lat_lng ON properties (latitude, longitude)
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

COMMENT ON COLUMN properties.latitude IS 'WGS84 latitude derived from address; null until geocoded';
COMMENT ON COLUMN properties.longitude IS 'WGS84 longitude derived from address; null until geocoded';
COMMENT ON COLUMN properties.geocoded_at IS 'Timestamp of last successful or attempted geocoding';
COMMENT ON COLUMN properties.geocoding_status IS 'PENDING | SUCCESS | FAILED | SKIPPED';
