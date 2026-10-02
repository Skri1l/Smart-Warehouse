package com.skr1l.backend.demandPlan.service;

import com.skr1l.backend.demandPlan.dto.DemandPlanRequestDto;
import com.skr1l.backend.demandPlan.dto.DemandPlanResponseDto;
import java.time.LocalDate;
import java.util.List;

public interface DemandPlanService {

    Long createDemandPlan(DemandPlanRequestDto dto);

    DemandPlanResponseDto getDemandPlanById(Long id);

    List<DemandPlanResponseDto> getAllDemandPlans();

    List<DemandPlanResponseDto> getDemandPlansByProductId(Long productId);

    List<DemandPlanResponseDto> getDemandPlansByRequiredDate(LocalDate requiredDate);

    List<DemandPlanResponseDto> getDemandPlansByDateRange(LocalDate from, LocalDate to);

    List<DemandPlanResponseDto> getFutureDemandPlans();

    DemandPlanResponseDto updateDemandPlan(Long id, DemandPlanRequestDto dto);

    void deleteDemandPlan(Long id);
}