package com.skr1l.backend.purchaseOrder.controller;

import com.skr1l.backend.purchaseOrder.dto.PurchaseOrderRequestDto;
import com.skr1l.backend.purchaseOrder.dto.PurchaseOrderResponseDto;
import com.skr1l.backend.purchaseOrder.enums.PurchaseOrderStatus;
import com.skr1l.backend.purchaseOrder.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Purchase Orders", description = "Purchase order management API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @Operation(summary = "Create a purchase order")
    @ApiResponse(responseCode = "201", description = "Purchase order created")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "404", description = "Replenishment recommendation not found")
    @ApiResponse(responseCode = "409", description = "Purchase order for this recommendation already exists")
    @PostMapping
    public ResponseEntity<Long> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderRequestDto dto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(purchaseOrderService.createPurchaseOrder(dto));
    }

    @Operation(summary = "Get purchase order by id")
    @ApiResponse(responseCode = "200", description = "Purchase order found")
    @ApiResponse(responseCode = "404", description = "Purchase order not found")
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponseDto> getPurchaseOrderById(@PathVariable Long id) {

        return ResponseEntity
                .ok(purchaseOrderService.getPurchaseOrderById(id));
    }

    @Operation(summary = "Get all purchase orders")
    @ApiResponse(responseCode = "200", description = "Purchase orders returned successfully")
    @GetMapping
    public ResponseEntity<List<PurchaseOrderResponseDto>> getAllPurchaseOrders() {

        return ResponseEntity
                .ok(purchaseOrderService.getAllPurchaseOrders());
    }

    @Operation(summary = "Get purchase orders by product id")
    @ApiResponse(responseCode = "200", description = "Purchase orders returned successfully")
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<PurchaseOrderResponseDto>> getPurchaseOrdersByProductId(
            @PathVariable Long productId) {

        return ResponseEntity.ok(purchaseOrderService
                .getPurchaseOrdersByProductId(productId));
    }

    @Operation(summary = "Get purchase order by recommendation id")
    @ApiResponse(responseCode = "200", description = "Purchase order found")
    @ApiResponse(responseCode = "404", description = "Purchase order not found")
    @GetMapping("/recommendation/{recommendationId}")
    public ResponseEntity<PurchaseOrderResponseDto> getPurchaseOrderByRecommendationId(
            @PathVariable Long recommendationId) {

        return ResponseEntity.ok(purchaseOrderService
                .getPurchaseOrderByRecommendationId(recommendationId));
    }

    @Operation(summary = "Get purchase orders by status")
    @ApiResponse(responseCode = "200", description = "Purchase orders returned successfully")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<PurchaseOrderResponseDto>> getPurchaseOrdersByStatus(
            @PathVariable PurchaseOrderStatus status) {

        return ResponseEntity
                .ok(purchaseOrderService.getPurchaseOrdersByStatus(status));
    }

    @Operation(summary = "Mark purchase order as ordered")
    @ApiResponse(responseCode = "204", description = "Purchase order marked as ordered")
    @ApiResponse(responseCode = "404", description = "Purchase order not found")
    @PatchMapping("/{id}/ordered")
    public ResponseEntity<Void> markAsOrdered(@PathVariable Long id) {
        purchaseOrderService.markAsOrdered(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark purchase order as received")
    @ApiResponse(responseCode = "204", description = "Purchase order marked as received")
    @ApiResponse(responseCode = "404", description = "Purchase order not found")
    @PatchMapping("/{id}/received")
    public ResponseEntity<Void> markAsReceived(@PathVariable Long id) {
        purchaseOrderService.markAsReceived(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cancel a purchase order")
    @ApiResponse(responseCode = "204", description = "Purchase order cancelled")
    @ApiResponse(responseCode = "404", description = "Purchase order not found")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelPurchaseOrder(@PathVariable Long id) {
        purchaseOrderService.cancelPurchaseOrder(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete a purchase order")
    @ApiResponse(responseCode = "204", description = "Purchase order deleted")
    @ApiResponse(responseCode = "404", description = "Purchase order not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePurchaseOrder(@PathVariable Long id) {
        purchaseOrderService.deletePurchaseOrder(id);

        return ResponseEntity.noContent().build();
    }
}