package com.skr1l.backend.product.service;

import com.skr1l.backend.product.dto.ProductRequestDto;
import com.skr1l.backend.product.dto.ProductResponseDto;
import java.util.List;

public interface ProductService {

    Long createProduct(ProductRequestDto dto);

    ProductResponseDto getProductById(Long id);

    ProductResponseDto getProductBySku(String sku);

    List<ProductResponseDto> getProductsByName(String name);

    List<ProductResponseDto> getAllProducts();

    ProductResponseDto updateProduct(Long id, ProductRequestDto dto);

    void deleteProduct(Long id);
}