package com.bajrix;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ListingRulesTest {

    @Test
    void minOrderQtyAboveStockIsRejected() {
        ApiException e = assertThrows(ApiException.class, () -> ListingService.validateStockAndMoq(5, 10));
        assertEquals(HttpStatus.BAD_REQUEST, e.status());
    }

    @Test
    void outOfStockListingMayKeepAnyMinOrderQty() {
        assertDoesNotThrow(() -> ListingService.validateStockAndMoq(0, 10));
    }

    @Test
    void minOrderQtyEqualToStockIsAllowed() {
        assertDoesNotThrow(() -> ListingService.validateStockAndMoq(10, 10));
    }
}
