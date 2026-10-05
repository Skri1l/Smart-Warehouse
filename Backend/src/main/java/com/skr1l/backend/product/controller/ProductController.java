package com.skr1l.backend.product.controller;

import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.product.dto.ProductRequestDto;
import com.skr1l.backend.product.dto.ProductResponseDto;
import com.skr1l.backend.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Products", description = "Product management API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private static final AppLogger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    @Operation(summary = "Get product by id")
    @ApiResponse(responseCode = "200", description = "Product found")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProduct(@PathVariable Long id) {
        log.info("getProduct controller");

        return ResponseEntity.ok(productService.getProductById(id));
    }

    @Operation(summary = "Get all products")
    @ApiResponse(responseCode = "200", description = "Products returned successfully")
    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        log.info("getAllProducts controller");

        return ResponseEntity.ok(productService.getAllProducts());
    }

    @Operation(summary = "Get product by SKU")
    @ApiResponse(responseCode = "200", description = "Product found")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductResponseDto> getProductBySku(@PathVariable String sku) {
        log.info("getProductBySku controller");

        return ResponseEntity.ok(productService.getProductBySku(sku));
    }

    @Operation(summary = "Search products by name")
    @ApiResponse(responseCode = "200", description = "Products returned successfully")
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponseDto>> getProductsByName(@RequestParam String name) {
        log.info("getProductsByName controller");

        return ResponseEntity.ok(productService.getProductsByName(name));
    }

    @Operation(summary = "Create a product")
    @ApiResponse(responseCode = "201", description = "Product created")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "409", description = "Product with this SKU already exists")
    @PostMapping
    public ResponseEntity<Long> createProduct(@Valid @RequestBody ProductRequestDto dto) {
        log.info("createProduct controller");

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productService.createProduct(dto));
    }

    @Operation(summary = "Update a product")
    @ApiResponse(responseCode = "200", description = "Product updated")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @ApiResponse(responseCode = "409", description = "Product with this SKU already exists")
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> updateProduct(@PathVariable Long id,
                                                            @Valid @RequestBody ProductRequestDto dto) {
        log.info("updateProduct controller");

        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    @Operation(summary = "Delete a product")
    @ApiResponse(responseCode = "204", description = "Product deleted")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.info("deleteProduct controller");

        productService.deleteProduct(id);

        return ResponseEntity.noContent().build();
    }
}