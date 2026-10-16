package com.skr1l.backend.replenishmentRecommendation.repository;

import com.skr1l.backend.replenishmentRecommendation.ReplenishmentRecommendation;
import com.skr1l.backend.replenishmentRecommendation.enums.RecommendationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReplenishmentRecommendationRepository extends JpaRepository<ReplenishmentRecommendation, Long> {

    List<ReplenishmentRecommendation> findByProductId(Long productId);

    List<ReplenishmentRecommendation> findByStatus(RecommendationStatus status);

    List<ReplenishmentRecommendation> findByRequiredBy(LocalDate requiredBy);

    List<ReplenishmentRecommendation> findByRequiredByBetween(LocalDate from, LocalDate to);
}
