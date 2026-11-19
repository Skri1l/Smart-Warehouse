package com.skr1l.backend.purchaseOrder.dto;

import com.skr1l.backend.purchaseOrder.enums.PurchaseOrderStatus;

import java.time.LocalDateTime;

public record PurchaseOrderResponseDto(
        Long id,
        Long recommendationId,
        Long productId,
        Integer quantity,
        PurchaseOrderStatus status,
        LocalDateTime createdAt) {
}