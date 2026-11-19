package com.skr1l.backend.product.dto;

public record ProductResponseDto(
        Long id,
        String sku,
        String name,
        String description) {
}