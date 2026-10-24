package com.bajrix;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/** All request/response shapes in one place. */
public final class Dto {
    private Dto() {}

    public record Seller(long id, String name, String city, String status) {}

    public record ProductSummary(long id, String name, String brand, String category, String unit,
                                 BigDecimal minPrice, int offerCount) {}

    public record PageResult<T>(List<T> items, int page, int size, long total) {}

    public record Offer(long listingId, long sellerId, String sellerName, String city, BigDecimal price,
                        int stock, int minOrderQty, boolean inStock) {}

    public record ProductDetail(long id, String name, String brand, String category, String unit,
                                List<Offer> offers) {}

    public record CatalogItem(long id, String name, String brand, String category, String unit) {}

    public record ListingView(long id, long productId, String productName, String unit, BigDecimal price,
                              int stock, int minOrderQty, long version) {}

    public record CreateListing(
            @NotNull Long productId,
            @NotNull @DecimalMin(value = "0.01", message = "must be at least 0.01")
            @Digits(integer = 10, fraction = 2, message = "max 2 decimal places") BigDecimal price,
            @NotNull @Min(value = 0, message = "cannot be negative") Integer stock,
            @NotNull @Min(value = 1, message = "must be at least 1") Integer minOrderQty) {}

    public record UpdateListing(
            @NotNull @DecimalMin(value = "0.01", message = "must be at least 0.01")
            @Digits(integer = 10, fraction = 2, message = "max 2 decimal places") BigDecimal price,
            @NotNull @Min(value = 0, message = "cannot be negative") Integer stock,
            @NotNull @Min(value = 1, message = "must be at least 1") Integer minOrderQty,
            @NotNull Long version) {}

    public record ErrorBody(String message) {}
}
