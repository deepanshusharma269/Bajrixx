package com.bajrix;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BuyerController {
    private final CatalogService catalog;

    public BuyerController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/products")
    Dto.PageResult<Dto.ProductSummary> search(@RequestParam(required = false) String q,
                                              @RequestParam(required = false) String category,
                                              @RequestParam(defaultValue = "name") String sort,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "12") int size) {
        return catalog.search(q, category, sort, page, size);
    }

    @GetMapping("/products/{id}")
    Dto.ProductDetail detail(@PathVariable long id) {
        return catalog.detail(id);
    }

    @GetMapping("/categories")
    List<String> categories() {
        return catalog.categories();
    }
}
