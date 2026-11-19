package com.skr1l.backend.stockMovement.repository;

import com.skr1l.backend.stockMovement.StockMovement;
import com.skr1l.backend.stockMovement.enums.StockMovementSource;
import com.skr1l.backend.stockMovement.enums.StockMovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByProductId(Long productId);

    List<StockMovement> findByShelfId(Long shelfId);

    List<StockMovement> findByType(StockMovementType type);

    List<StockMovement> findBySource(StockMovementSource source);

    List<StockMovement> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}