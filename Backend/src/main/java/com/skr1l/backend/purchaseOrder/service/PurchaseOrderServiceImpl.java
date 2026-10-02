package com.skr1l.backend.purchaseOrder.service;

import com.skr1l.backend.exception.purchaseOrderExceptions.PurchaseOrderAlreadyExistsException;
import com.skr1l.backend.exception.purchaseOrderExceptions.PurchaseOrderIsNotFoundException;
import com.skr1l.backend.exception.ReplenishmentRecommendationExceptions.ReplenishmentRecommendationIsNotFoundException;
import com.skr1l.backend.product.Product;
import com.skr1l.backend.purchaseOrder.PurchaseOrder;
import com.skr1l.backend.purchaseOrder.dto.PurchaseOrderRequestDto;
import com.skr1l.backend.purchaseOrder.dto.PurchaseOrderResponseDto;
import com.skr1l.backend.purchaseOrder.enums.PurchaseOrderStatus;
import com.skr1l.backend.purchaseOrder.repository.PurchaseOrderRepository;
import com.skr1l.backend.replenishmentRecommendation.ReplenishmentRecommendation;
import com.skr1l.backend.replenishmentRecommendation.repository.ReplenishmentRecommendationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;

    private final ReplenishmentRecommendationRepository recommendationRepository;

    @Override
    @Transactional
    public Long createPurchaseOrder(PurchaseOrderRequestDto dto) {
        Objects.requireNonNull(dto, "dto is null");

        if (purchaseOrderRepository.existsByRecommendationId(dto.recommendationId())) {
            throw new PurchaseOrderAlreadyExistsException(
                    "Purchase order for recommendation with id "
                            + dto.recommendationId() + " already exists");
        }

        ReplenishmentRecommendation recommendation = getRecommendationEntityById(dto.recommendationId());
        Product product = recommendation.getProduct();

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setRecommendation(recommendation);
        purchaseOrder.setProduct(product);
        purchaseOrder.setQuantity(dto.quantity());
        purchaseOrder.setStatus(PurchaseOrderStatus.CREATED);
        purchaseOrder.setCreatedAt(LocalDateTime.now());

        purchaseOrderRepository.save(purchaseOrder);

        return purchaseOrder.getId();
    }

    @Override
    public PurchaseOrderResponseDto getPurchaseOrderById(Long id) {
        Objects.requireNonNull(id, "id is null");

        return toResponseDto(getPurchaseOrderEntityById(id));
    }

    @Override
    public PurchaseOrderResponseDto getPurchaseOrderByRecommendationId(Long recommendationId) {
        Objects.requireNonNull(recommendationId, "recommendationId is null");

        PurchaseOrder purchaseOrder = purchaseOrderRepository.findByRecommendationId(recommendationId)
                .orElseThrow(() -> new PurchaseOrderIsNotFoundException(
                        "Purchase order for recommendation with id " + recommendationId + " is not found"));

        return toResponseDto(purchaseOrder);
    }

    @Override
    public List<PurchaseOrderResponseDto> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<PurchaseOrderResponseDto> getPurchaseOrdersByProductId(Long productId) {
        Objects.requireNonNull(productId, "productId is null");

        return purchaseOrderRepository.findByProductId(productId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<PurchaseOrderResponseDto> getPurchaseOrdersByStatus(PurchaseOrderStatus status) {
        Objects.requireNonNull(status, "status is null");

        return purchaseOrderRepository.findByStatus(status)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public void markAsOrdered(Long id) {
        PurchaseOrder purchaseOrder = getPurchaseOrderEntityById(id);
        purchaseOrder.setStatus(PurchaseOrderStatus.ORDERED);
    }

    @Override
    @Transactional
    public void markAsReceived(Long id) {
        PurchaseOrder purchaseOrder = getPurchaseOrderEntityById(id);
        purchaseOrder.setStatus(PurchaseOrderStatus.RECEIVED);
    }

    @Override
    @Transactional
    public void cancelPurchaseOrder(Long id) {
        PurchaseOrder purchaseOrder = getPurchaseOrderEntityById(id);
        purchaseOrder.setStatus(PurchaseOrderStatus.CANCELLED);
    }

    @Override
    @Transactional
    public void deletePurchaseOrder(Long id) {
        PurchaseOrder purchaseOrder = getPurchaseOrderEntityById(id);
        purchaseOrderRepository.delete(purchaseOrder);
    }

    private PurchaseOrder getPurchaseOrderEntityById(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new PurchaseOrderIsNotFoundException(
                        "Purchase order with id " + id + " is not found"));
    }

    private ReplenishmentRecommendation getRecommendationEntityById(Long id) {
        return recommendationRepository.findById(id)
                .orElseThrow(() -> new ReplenishmentRecommendationIsNotFoundException(
                        "Replenishment recommendation with id " + id + " is not found"));
    }

    private PurchaseOrderResponseDto toResponseDto(PurchaseOrder purchaseOrder) {
        return new PurchaseOrderResponseDto(
                purchaseOrder.getId(),
                purchaseOrder.getRecommendation().getId(),
                purchaseOrder.getProduct().getId(),
                purchaseOrder.getQuantity(),
                purchaseOrder.getStatus(),
                purchaseOrder.getCreatedAt());
    }
}