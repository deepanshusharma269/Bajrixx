# BajriX Marketplace (take-home)

Multi-seller marketplace: buyers compare seller offers per product; sellers manage their own listings.
Stack: React (Vite) · Spring Boot 3 (Java 17, plain JDBC) · PostgreSQL 16.

## Run it

Prereqs: Java 17+, Maven, Node 18+, Docker (for Postgres).

```bash
docker compose up -d                 # Postgres on :5432 (db: bajrix)
cd backend && mvn spring-boot:run    # API on :8080, creates schema + seeds sample data on start
cd frontend && npm install && npm run dev   # UI on http://localhost:5173 (proxies /api to :8080)
```

Tests: `cd backend && mvn test`.

Using the app: **Buy** tab = browse/search/filter/sort/paginate, click a product to compare sellers.
**Sell** tab = pick a seller in the "Acting as" dropdown (mock login) to add, edit, or stop selling listings.

## Sample data (backend/src/main/resources/schema.sql, data.sql)

Applied on every start; both are idempotent (`IF NOT EXISTS`, `ON CONFLICT DO NOTHING`), so restarting never overwrites edits.
- 5 sellers: 3 APPROVED, 1 PENDING (New Star Cement Depot), 1 REJECTED (Quick Deals Co).
- UltraTech PPC Cement has 5 listings (₹300–₹405, varied stock/MOQ); the PENDING and REJECTED sellers' cheaper offers (₹350, ₹300) are hidden from buyers.
- ACC cement has an out-of-stock listing (shown greyed out in the product detail, ignored in search/min price).

## Key decisions

- **Product vs listing**: `products` (catalogue) and `seller_listings` (seller, product, price, stock, min order qty). `UNIQUE (product_id, seller_id)` prevents duplicate listings.
- **Seller status**: buyers only ever see listings from APPROVED sellers (enforced in the queries). PENDING/REJECTED sellers can open the Sell tab and view their listings, but every write returns 403.
- **Authorization**: mock login via `X-Seller-Id` header. Every seller query includes `seller_id = :sid`, so another seller's listing id behaves as "not found". A real version would derive the seller id from a JWT/session, not a client-sent header.
- **Concurrency**: optimistic locking with a `version` column. The client sends the version it loaded; `UPDATE ... WHERE id=? AND seller_id=? AND version=?` bumps it. Zero rows updated means someone else changed it, so the API returns 409 and the UI reloads the latest values. Creating a listing uses a single `INSERT ... ON CONFLICT` so two simultaneous "add" calls cannot both succeed.
- **Stop selling** is a soft delete (`active=false`). Adding the same product again revives the row.
- **Search**: server-side filtering, sorting and pagination (page size capped at 50). Name search uses `LIKE` backed by a `pg_trgm` GIN index; there are indexes on category, seller and live listings.
- **Why plain JDBC (`JdbcClient`)**: the buyer query is an aggregate (min price, offer count) with dynamic filters, and the version-checked UPDATE is clearer as explicit SQL than through an ORM.

## Business rules / assumptions

- price ≥ 0.01 with at most 2 decimals; stock ≥ 0; min order qty ≥ 1.
- If stock > 0, min order qty cannot exceed stock. Stock 0 means out of stock and is allowed with any min order qty.
- Products with no in-stock listing from an approved seller do not appear in search; the detail page still lists their offers (out-of-stock ones greyed out).
- Sellers can only list existing catalogue products. Creating new catalogue products is not supported.


## With more time

- Integration tests (Testcontainers) for the 409 conflict, duplicate create, and cross-seller access.
- Real auth and an admin flow to approve/reject sellers.
- Show "current" vs "your" version in the UI when an edit conflicts, instead of just reloading.
- Product images, unit-aware price comparison, seller ratings/delivery area.

