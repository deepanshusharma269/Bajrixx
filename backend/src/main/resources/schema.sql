CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS sellers (
  id     BIGSERIAL PRIMARY KEY,
  name   VARCHAR(120) NOT NULL UNIQUE,
  city   VARCHAR(80)  NOT NULL,
  status VARCHAR(10)  NOT NULL CHECK (status IN ('APPROVED', 'PENDING', 'REJECTED'))
);

-- Product = catalogue entry, independent of who sells it.
CREATE TABLE IF NOT EXISTS products (
  id       BIGSERIAL PRIMARY KEY,
  name     VARCHAR(200) NOT NULL UNIQUE,
  brand    VARCHAR(80)  NOT NULL,
  category VARCHAR(60)  NOT NULL,
  unit     VARCHAR(20)  NOT NULL
);

-- Seller listing = one seller's offer for one product.
-- Soft delete via active=false. version is used for optimistic locking.
CREATE TABLE IF NOT EXISTS seller_listings (
  id            BIGSERIAL PRIMARY KEY,
  product_id    BIGINT         NOT NULL REFERENCES products (id),
  seller_id     BIGINT         NOT NULL REFERENCES sellers (id),
  price         NUMERIC(12, 2) NOT NULL CHECK (price > 0),
  stock         INT            NOT NULL CHECK (stock >= 0),
  min_order_qty INT            NOT NULL CHECK (min_order_qty >= 1),
  active        BOOLEAN        NOT NULL DEFAULT TRUE,
  version       BIGINT         NOT NULL DEFAULT 0,
  updated_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
  UNIQUE (product_id, seller_id)
);

CREATE INDEX IF NOT EXISTS idx_listings_seller ON seller_listings (seller_id);
CREATE INDEX IF NOT EXISTS idx_listings_live ON seller_listings (product_id, price) WHERE active AND stock > 0;
CREATE INDEX IF NOT EXISTS idx_products_category ON products (category);
CREATE INDEX IF NOT EXISTS idx_products_name_trgm ON products USING gin (lower(name) gin_trgm_ops);
