package com.skr1l.backend.purchaseOrder.controller;

import com.skr1l.backend.purchaseOrder.dto.PurchaseOrderRequestDto;
import com.skr1l.backend.purchaseOrder.dto.PurchaseOrderResponseDto;
import com.skr1l.backend.purchaseOrder.enums.PurchaseOrderStatus;
import com.skr1l.backend.purchaseOrder.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping
    public ResponseEntity<Long> createPurchaseOrder(@Valid @RequestBody PurchaseOrderRequestDto dto) {

        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseOrderService.createPurchaseOrder(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponseDto> getPurchaseOrderById(@PathVariable Long id) {

        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrderById(id));
    }

    @GetMapping
    public ResponseEntity<List<PurchaseOrderResponseDto>> getAllPurchaseOrders() {

        return ResponseEntity.ok(purchaseOrderService.getAllPurchaseOrders());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<PurchaseOrderResponseDto>> getPurchaseOrdersByProductId(
            @PathVariable Long productId) {

        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrdersByProductId(productId));
    }

    @GetMapping("/recommendation/{recommendationId}")
    public ResponseEntity<PurchaseOrderResponseDto> getPurchaseOrderByRecommendationId(
            @PathVariable Long recommendationId) {

        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrderByRecommendationId(recommendationId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PurchaseOrderResponseDto>> getPurchaseOrdersByStatus(
            @PathVariable PurchaseOrderStatus status) {

        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrdersByStatus(status));
    }

    @PatchMapping("/{id}/ordered")
    public ResponseEntity<Void> markAsOrdered(@PathVariable Long id) {
        purchaseOrderService.markAsOrdered(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/received")
    public ResponseEntity<Void> markAsReceived(@PathVariable Long id) {
        purchaseOrderService.markAsReceived(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelPurchaseOrder(@PathVariable Long id) {
        purchaseOrderService.cancelPurchaseOrder(id);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePurchaseOrder(@PathVariable Long id) {
        purchaseOrderService.deletePurchaseOrder(id);

        return ResponseEntity.noContent().build();
    }
}