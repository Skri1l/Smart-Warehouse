package com.skr1l.backend.purchaseOrder.service;

import com.skr1l.backend.purchaseOrder.dto.PurchaseOrderRequestDto;
import com.skr1l.backend.purchaseOrder.dto.PurchaseOrderResponseDto;
import com.skr1l.backend.purchaseOrder.enums.PurchaseOrderStatus;
import java.util.List;

public interface PurchaseOrderService {

    Long createPurchaseOrder(PurchaseOrderRequestDto dto);

    PurchaseOrderResponseDto getPurchaseOrderById(Long id);

    PurchaseOrderResponseDto getPurchaseOrderByRecommendationId(Long recommendationId);

    List<PurchaseOrderResponseDto> getAllPurchaseOrders();

    List<PurchaseOrderResponseDto> getPurchaseOrdersByProductId(Long productId);

    List<PurchaseOrderResponseDto> getPurchaseOrdersByStatus(PurchaseOrderStatus status);

    void markAsOrdered(Long id);

    void markAsReceived(Long id);

    void cancelPurchaseOrder(Long id);

    void deletePurchaseOrder(Long id);
}