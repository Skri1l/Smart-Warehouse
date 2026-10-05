package com.skr1l.backend.purchaseOrder.repository;

import com.skr1l.backend.purchaseOrder.PurchaseOrder;
import com.skr1l.backend.purchaseOrder.enums.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findByProductId(Long productId);

    List<PurchaseOrder> findByStatus(PurchaseOrderStatus status);

    Optional<PurchaseOrder> findByRecommendationId(Long recommendationId);

    boolean existsByRecommendationId(Long recommendationId);
}