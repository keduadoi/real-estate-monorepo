-- Create property_prices table (current price per property)
CREATE TABLE property_prices (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT NOT NULL UNIQUE,
    current_price   NUMERIC(19, 2) NOT NULL CHECK (current_price > 0),
    currency        VARCHAR(3) NOT NULL DEFAULT 'VND',
    last_updated_by VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for looking up price by property
CREATE INDEX idx_property_prices_property_id ON property_prices(property_id);

-- Create price_history table (audit trail)
CREATE TABLE price_history (
    id              BIGSERIAL PRIMARY KEY,
    property_id     BIGINT NOT NULL,
    old_price       NUMERIC(19, 2),
    new_price       NUMERIC(19, 2) NOT NULL CHECK (new_price > 0),
    changed_by      VARCHAR(255),
    changed_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    reason          VARCHAR(500)
);

-- Index for looking up history by property
CREATE INDEX idx_price_history_property_id ON price_history(property_id);

-- Index for ordering history by date
CREATE INDEX idx_price_history_changed_at ON price_history(changed_at DESC);

COMMENT ON TABLE property_prices IS 'Current price for each property';
COMMENT ON TABLE price_history IS 'Audit trail of all price changes';
COMMENT ON COLUMN property_prices.property_id IS 'Reference to property ID from property-service';
COMMENT ON COLUMN price_history.old_price IS 'Previous price, null for initial price entry';
