package com.bajrix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Buyer-facing reads. Only APPROVED sellers' active listings are ever exposed. */
@Service
public class CatalogService {
    private final JdbcClient jdbc;

    public CatalogService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Dto.PageResult<Dto.ProductSummary> search(String q, String category, String sort, int page, int size) {
        size = Math.min(Math.max(size, 1), 50);
        page = Math.max(page, 0);

        StringBuilder where = new StringBuilder(" WHERE s.status = 'APPROVED' AND l.active AND l.stock > 0");
        Map<String, Object> params = new HashMap<>();
        if (q != null && !q.isBlank()) {
            where.append(" AND (lower(p.name) LIKE :q OR lower(p.brand) LIKE :q)");
            params.put("q", "%" + escapeLike(q.trim().toLowerCase()) + "%");
        }
        if (category != null && !category.isBlank()) {
            where.append(" AND p.category = :category");
            params.put("category", category);
        }
        String from = " FROM products p JOIN seller_listings l ON l.product_id = p.id"
                + " JOIN sellers s ON s.id = l.seller_id" + where;

        String order = switch (sort == null ? "" : sort) {
            case "price_asc" -> "min_price ASC, p.id";
            case "price_desc" -> "min_price DESC, p.id";
            default -> "p.name, p.id";
        };

        long total = jdbc.sql("SELECT count(DISTINCT p.id)" + from).params(params).query(Long.class).single();
        List<Dto.ProductSummary> items = jdbc.sql(
                        "SELECT p.id, p.name, p.brand, p.category, p.unit, min(l.price) AS min_price, count(*) AS offers"
                                + from + " GROUP BY p.id ORDER BY " + order + " LIMIT :limit OFFSET :offset")
                .params(params)
                .param("limit", size)
                .param("offset", (long) page * size)
                .query((rs, i) -> new Dto.ProductSummary(rs.getLong("id"), rs.getString("name"),
                        rs.getString("brand"), rs.getString("category"), rs.getString("unit"),
                        rs.getBigDecimal("min_price"), rs.getInt("offers")))
                .list();
        return new Dto.PageResult<>(items, page, size, total);
    }

    public List<String> categories() {
        return jdbc.sql("SELECT DISTINCT category FROM products ORDER BY category").query(String.class).list();
    }

    public Dto.ProductDetail detail(long id) {
        Dto.CatalogItem p = jdbc.sql("SELECT id, name, brand, category, unit FROM products WHERE id = :id")
                .param("id", id)
                .query((rs, i) -> new Dto.CatalogItem(rs.getLong("id"), rs.getString("name"),
                        rs.getString("brand"), rs.getString("category"), rs.getString("unit")))
                .optional()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));

        List<Dto.Offer> offers = jdbc.sql("""
                        SELECT l.id, s.id AS sid, s.name, s.city, l.price, l.stock, l.min_order_qty
                        FROM seller_listings l JOIN sellers s ON s.id = l.seller_id
                        WHERE l.product_id = :id AND l.active AND s.status = 'APPROVED'
                        ORDER BY (l.stock > 0) DESC, l.price ASC
                        """)
                .param("id", id)
                .query((rs, i) -> new Dto.Offer(rs.getLong("id"), rs.getLong("sid"), rs.getString("name"),
                        rs.getString("city"), rs.getBigDecimal("price"), rs.getInt("stock"),
                        rs.getInt("min_order_qty"), rs.getInt("stock") > 0))
                .list();
        return new Dto.ProductDetail(p.id(), p.name(), p.brand(), p.category(), p.unit(), offers);
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
