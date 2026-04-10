-- Add column
ALTER TABLE product ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- Backfill data
UPDATE product
SET search_vector =
        to_tsvector('simple',
                    coalesce(product_name, '') || ' ' ||
                    coalesce(product_description, '')
        );

-- Create index
CREATE INDEX IF NOT EXISTS idx_products_search
    ON product USING GIN(search_vector);

-- Trigger function
CREATE OR REPLACE FUNCTION update_product_search_vector()
RETURNS trigger AS $$
BEGIN
  NEW.search_vector :=
    to_tsvector('simple',
      coalesce(NEW.name, '') || ' ' ||
      coalesce(NEW.description, '')
    );
RETURN NEW;
END
$$ LANGUAGE plpgsql;

-- Trigger
DROP TRIGGER IF EXISTS product_search_vector_trigger ON product;

CREATE TRIGGER product_search_vector_trigger
    BEFORE INSERT OR UPDATE
                         ON product
                         FOR EACH ROW
                         EXECUTE FUNCTION update_product_search_vector();
-- Enable trigram extension (for fuzzy search)
CREATE EXTENSION IF NOT EXISTS pg_trgm;
