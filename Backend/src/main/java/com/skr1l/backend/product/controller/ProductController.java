package com.skr1l.backend.product.controller;

import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.product.dto.ProductRequestDto;
import com.skr1l.backend.product.dto.ProductResponseDto;
import com.skr1l.backend.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private static final AppLogger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProduct(@PathVariable Long id) {
        log.info("getProduct controller");

        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        log.info("getAllProducts controller");

        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductResponseDto> getProductBySku(@PathVariable String sku) {
        log.info("getProductBySku controller");

        return ResponseEntity.ok(productService.getProductBySku(sku));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponseDto>> getProductsByName(@RequestParam String name) {
        log.info("getProductsByName controller");

        return ResponseEntity.ok(productService.getProductsByName(name));
    }

    @PostMapping
    public ResponseEntity<Long> createProduct(@Valid @RequestBody ProductRequestDto dto) {
        log.info("createProduct controller");

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productService.createProduct(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> updateProduct(@PathVariable Long id,
                                                            @Valid @RequestBody ProductRequestDto dto) {
        log.info("updateProduct controller");

        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.info("deleteProduct controller");

        productService.deleteProduct(id);

        return ResponseEntity.noContent().build();
    }
}