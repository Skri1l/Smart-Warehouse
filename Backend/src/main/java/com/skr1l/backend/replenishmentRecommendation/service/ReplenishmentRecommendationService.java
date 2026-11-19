package com.skr1l.backend.replenishmentRecommendation.service;

import com.skr1l.backend.replenishmentRecommendation.dto.ReplenishmentRecommendationRequestDto;
import com.skr1l.backend.replenishmentRecommendation.dto.ReplenishmentRecommendationResponseDto;
import com.skr1l.backend.replenishmentRecommendation.enums.RecommendationStatus;
import java.time.LocalDate;
import java.util.List;

public interface ReplenishmentRecommendationService {

    Long createRecommendation(ReplenishmentRecommendationRequestDto dto);

    ReplenishmentRecommendationResponseDto getRecommendationById(Long id);

    List<ReplenishmentRecommendationResponseDto> getAllRecommendations();

    List<ReplenishmentRecommendationResponseDto> getRecommendationsByProductId(Long productId);

    List<ReplenishmentRecommendationResponseDto> getRecommendationsByStatus(RecommendationStatus status);

    List<ReplenishmentRecommendationResponseDto> getRecommendationsByRequiredBy(LocalDate requiredBy);

    List<ReplenishmentRecommendationResponseDto> getRecommendationsByDateRange(LocalDate from, LocalDate to);

    ReplenishmentRecommendationResponseDto updateRecommendation(Long id,
                                                                ReplenishmentRecommendationRequestDto dto);

    void approveRecommendation(Long id);

    void rejectRecommendation(Long id);

    void markRecommendationAsOrdered(Long id);

    void completeRecommendation(Long id);

    void deleteRecommendation(Long id);
}