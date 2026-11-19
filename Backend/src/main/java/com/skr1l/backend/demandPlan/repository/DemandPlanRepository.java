package com.skr1l.backend.demandPlan.repository;

import com.skr1l.backend.demandPlan.DemandPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DemandPlanRepository extends JpaRepository<DemandPlan, Long> {

    List<DemandPlan> findByProductId(Long productId);

    List<DemandPlan> findByRequiredDate(LocalDate requiredDate);

    List<DemandPlan> findByRequiredDateBetween(LocalDate from, LocalDate to);

    List<DemandPlan> findByRequiredDateAfter(LocalDate date);
}