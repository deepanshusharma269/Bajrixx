package com.bajrix;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SellerController {
    private static final String SELLER_HEADER = "X-Seller-Id";
    private final ListingService listings;

    public SellerController(ListingService listings) {
        this.listings = listings;
    }

    /** Only used by the mock "log in as seller" dropdown. */
    @GetMapping("/sellers")
    List<Dto.Seller> sellers() {
        return listings.allSellers();
    }

    @GetMapping("/seller/listings")
    List<Dto.ListingView> mine(@RequestHeader(value = SELLER_HEADER, required = false) Long sellerId) {
        return listings.list(sellerId);
    }

    @GetMapping("/seller/catalog")
    List<Dto.CatalogItem> catalog(@RequestHeader(value = SELLER_HEADER, required = false) Long sellerId,
                                  @RequestParam(required = false) String q) {
        return listings.catalog(sellerId, q);
    }

    @PostMapping("/seller/listings")
    @ResponseStatus(HttpStatus.CREATED)
    Dto.ListingView create(@RequestHeader(value = SELLER_HEADER, required = false) Long sellerId,
                           @Valid @RequestBody Dto.CreateListing body) {
        return listings.create(sellerId, body);
    }

    @PutMapping("/seller/listings/{id}")
    Dto.ListingView update(@RequestHeader(value = SELLER_HEADER, required = false) Long sellerId,
                           @PathVariable long id, @Valid @RequestBody Dto.UpdateListing body) {
        return listings.update(sellerId, id, body);
    }

    @DeleteMapping("/seller/listings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void stop(@RequestHeader(value = SELLER_HEADER, required = false) Long sellerId, @PathVariable long id) {
        listings.stopSelling(sellerId, id);
    }
}
