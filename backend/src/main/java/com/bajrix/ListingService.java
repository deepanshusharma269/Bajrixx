package com.bajrix;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Seller-side operations. Every query is scoped by seller_id, so a seller can never touch another
 * seller's rows. Concurrent edits are handled with an optimistic version check inside the UPDATE.
 */
@Service
public class ListingService {
    private final JdbcClient jdbc;

    private static final RowMapper<Dto.ListingView> LISTING = (rs, i) -> new Dto.ListingView(
            rs.getLong("id"), rs.getLong("product_id"), rs.getString("name"), rs.getString("unit"),
            rs.getBigDecimal("price"), rs.getInt("stock"), rs.getInt("min_order_qty"), rs.getLong("version"));

    private static final String LISTING_SELECT = """
            SELECT l.id, l.product_id, p.name, p.unit, l.price, l.stock, l.min_order_qty, l.version
            FROM seller_listings l JOIN products p ON p.id = l.product_id
            """;

    public ListingService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Dto.Seller> allSellers() {
        return jdbc.sql("SELECT id, name, city, status FROM sellers ORDER BY id").query(this::mapSeller).list();
    }

    private Dto.Seller mapSeller(java.sql.ResultSet rs, int i) throws java.sql.SQLException {
        return new Dto.Seller(rs.getLong("id"), rs.getString("name"), rs.getString("city"), rs.getString("status"));
    }

    /** Mock auth: identify the seller from the X-Seller-Id header. */
    public Dto.Seller requireSeller(Long sellerId) {
        if (sellerId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "X-Seller-Id header is required");
        }
        return jdbc.sql("SELECT id, name, city, status FROM sellers WHERE id = :id")
                .param("id", sellerId).query(this::mapSeller).optional()
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Unknown seller"));
    }

    /** Only APPROVED sellers may change data; pending/rejected sellers get read-only access. */
    private Dto.Seller requireApproved(Long sellerId) {
        Dto.Seller s = requireSeller(sellerId);
        if (!"APPROVED".equals(s.status())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Your seller account is " + s.status() + "; only approved sellers can manage listings");
        }
        return s;
    }

    public List<Dto.ListingView> list(Long sellerId) {
        requireSeller(sellerId);
        return jdbc.sql(LISTING_SELECT + " WHERE l.seller_id = :sid AND l.active ORDER BY p.name")
                .param("sid", sellerId).query(LISTING).list();
    }

    /** Products this seller is not already actively selling. */
    public List<Dto.CatalogItem> catalog(Long sellerId, String q) {
        requireSeller(sellerId);
        String like = "%" + (q == null ? "" : q.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")) + "%";
        return jdbc.sql("""
                        SELECT p.id, p.name, p.brand, p.category, p.unit FROM products p
                        WHERE lower(p.name) LIKE :q
                          AND NOT EXISTS (SELECT 1 FROM seller_listings l
                                          WHERE l.product_id = p.id AND l.seller_id = :sid AND l.active)
                        ORDER BY p.name LIMIT 20
                        """)
                .param("q", like).param("sid", sellerId)
                .query((rs, i) -> new Dto.CatalogItem(rs.getLong("id"), rs.getString("name"),
                        rs.getString("brand"), rs.getString("category"), rs.getString("unit")))
                .list();
    }

    public Dto.ListingView create(Long sellerId, Dto.CreateListing req) {
        requireApproved(sellerId);
        validateStockAndMoq(req.stock(), req.minOrderQty());

        Boolean productExists = jdbc.sql("SELECT EXISTS (SELECT 1 FROM products WHERE id = :id)")
                .param("id", req.productId()).query(Boolean.class).single();
        if (!productExists) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Product not found");
        }

        // Atomic: new row, or revive a previously stopped listing. If an ACTIVE one already exists
        // the WHERE clause fails, nothing is returned, and we report a duplicate (race-safe).
        Long id = jdbc.sql("""
                        INSERT INTO seller_listings (product_id, seller_id, price, stock, min_order_qty)
                        VALUES (:pid, :sid, :price, :stock, :moq)
                        ON CONFLICT (product_id, seller_id) DO UPDATE
                          SET price = EXCLUDED.price, stock = EXCLUDED.stock, min_order_qty = EXCLUDED.min_order_qty,
                              active = TRUE, version = seller_listings.version + 1, updated_at = now()
                          WHERE seller_listings.active = FALSE
                        RETURNING id
                        """)
                .param("pid", req.productId()).param("sid", sellerId).param("price", req.price())
                .param("stock", req.stock()).param("moq", req.minOrderQty())
                .query(Long.class).optional()
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "You already sell this product"));
        return get(sellerId, id);
    }

    public Dto.ListingView update(Long sellerId, long id, Dto.UpdateListing req) {
        requireApproved(sellerId);
        validateStockAndMoq(req.stock(), req.minOrderQty());

        int n = jdbc.sql("""
                        UPDATE seller_listings
                        SET price = :price, stock = :stock, min_order_qty = :moq,
                            version = version + 1, updated_at = now()
                        WHERE id = :id AND seller_id = :sid AND active AND version = :version
                        """)
                .param("price", req.price()).param("stock", req.stock()).param("moq", req.minOrderQty())
                .param("id", id).param("sid", sellerId).param("version", req.version())
                .update();
        if (n == 0) {
            // Either it is not yours / not active (404) or someone changed it since you loaded it (409).
            get(sellerId, id);
            throw new ApiException(HttpStatus.CONFLICT,
                    "This listing was changed by someone else. Reload and try again.");
        }
        return get(sellerId, id);
    }

    /** "Stop selling": soft delete so history and the unique (product, seller) key stay intact. */
    public void stopSelling(Long sellerId, long id) {
        requireApproved(sellerId);
        int n = jdbc.sql("""
                        UPDATE seller_listings SET active = FALSE, version = version + 1, updated_at = now()
                        WHERE id = :id AND seller_id = :sid AND active
                        """)
                .param("id", id).param("sid", sellerId).update();
        if (n == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Listing not found");
        }
    }

    private Dto.ListingView get(Long sellerId, long id) {
        return jdbc.sql(LISTING_SELECT + " WHERE l.id = :id AND l.seller_id = :sid AND l.active")
                .param("id", id).param("sid", sellerId).query(LISTING).optional()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Listing not found"));
    }

    /** A buyable listing must be able to satisfy its own minimum order. */
    static void validateStockAndMoq(int stock, int minOrderQty) {
        if (stock > 0 && minOrderQty > stock) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "minOrderQty cannot exceed stock");
        }
    }
}
