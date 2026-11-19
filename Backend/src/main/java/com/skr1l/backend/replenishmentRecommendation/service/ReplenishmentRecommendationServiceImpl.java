package com.skr1l.backend.replenishmentRecommendation.service;

import com.skr1l.backend.exception.productExceptions.ProductIsNotFoundException;
import com.skr1l.backend.exception.ReplenishmentRecommendationExceptions.ReplenishmentRecommendationIsNotFoundException;
import com.skr1l.backend.product.Product;
import com.skr1l.backend.product.repository.ProductRepository;
import com.skr1l.backend.replenishmentRecommendation.ReplenishmentRecommendation;
import com.skr1l.backend.replenishmentRecommendation.dto.ReplenishmentRecommendationRequestDto;
import com.skr1l.backend.replenishmentRecommendation.dto.ReplenishmentRecommendationResponseDto;
import com.skr1l.backend.replenishmentRecommendation.enums.RecommendationStatus;
import com.skr1l.backend.replenishmentRecommendation.repository.ReplenishmentRecommendationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReplenishmentRecommendationServiceImpl implements ReplenishmentRecommendationService {

    private final ReplenishmentRecommendationRepository recommendationRepository;
    private final ProductRepository productRepository;


    @Override
    @Transactional
    public Long createRecommendation(ReplenishmentRecommendationRequestDto dto) {
        Objects.requireNonNull(dto, "dto is null");

        Product product = getProductEntityById(dto.productId());

        ReplenishmentRecommendation recommendation = new ReplenishmentRecommendation();
        recommendation.setProduct(product);
        recommendation.setRecommendedQuantity(dto.recommendedQuantity());
        recommendation.setRequiredBy(dto.requiredBy());
        recommendation.setReason(dto.reason());
        recommendation.setStatus(RecommendationStatus.NEW);
        recommendation.setCreatedAt(LocalDateTime.now());

        recommendationRepository.save(recommendation);

        return recommendation.getId();
    }

    @Override
    public ReplenishmentRecommendationResponseDto getRecommendationById(Long id) {
        Objects.requireNonNull(id, "id is null");

        return toResponseDto(getRecommendationEntityById(id));
    }

    @Override
    public List<ReplenishmentRecommendationResponseDto> getAllRecommendations() {

        return recommendationRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<ReplenishmentRecommendationResponseDto> getRecommendationsByProductId(Long productId) {
        Objects.requireNonNull(productId, "productId is null");

        return recommendationRepository.findByProductId(productId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<ReplenishmentRecommendationResponseDto> getRecommendationsByStatus(
            RecommendationStatus status) {

        Objects.requireNonNull(status, "status is null");

        return recommendationRepository.findByStatus(status)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<ReplenishmentRecommendationResponseDto> getRecommendationsByRequiredBy(LocalDate requiredBy) {
        Objects.requireNonNull(requiredBy, "requiredBy is null");

        return recommendationRepository.findByRequiredBy(requiredBy)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<ReplenishmentRecommendationResponseDto> getRecommendationsByDateRange(
            LocalDate from, LocalDate to) {

        Objects.requireNonNull(from, "from is null");
        Objects.requireNonNull(to, "to is null");

        return recommendationRepository.findByRequiredByBetween(from, to)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public ReplenishmentRecommendationResponseDto updateRecommendation(
            Long id, ReplenishmentRecommendationRequestDto dto) {

        Objects.requireNonNull(id, "id is null");
        Objects.requireNonNull(dto, "dto is null");

        ReplenishmentRecommendation recommendation = getRecommendationEntityById(id);
        Product product = getProductEntityById(dto.productId());

        recommendation.setProduct(product);
        recommendation.setRecommendedQuantity(dto.recommendedQuantity());
        recommendation.setRequiredBy(dto.requiredBy());
        recommendation.setReason(dto.reason());

        return toResponseDto(recommendation);
    }

    @Override
    @Transactional
    public void approveRecommendation(Long id) {
        Objects.requireNonNull(id, "id is null");

        ReplenishmentRecommendation recommendation = getRecommendationEntityById(id);
        recommendation.setStatus(RecommendationStatus.APPROVED);
    }

    @Override
    @Transactional
    public void rejectRecommendation(Long id) {
        Objects.requireNonNull(id, "id is null");

        ReplenishmentRecommendation recommendation = getRecommendationEntityById(id);
        recommendation.setStatus(RecommendationStatus.REJECTED);
    }

    @Override
    @Transactional
    public void markRecommendationAsOrdered(Long id) {
        Objects.requireNonNull(id, "id is null");

        ReplenishmentRecommendation recommendation = getRecommendationEntityById(id);
        recommendation.setStatus(RecommendationStatus.ORDERED);
    }

    @Override
    @Transactional
    public void completeRecommendation(Long id) {
        Objects.requireNonNull(id, "id is null");

        ReplenishmentRecommendation recommendation = getRecommendationEntityById(id);
        recommendation.setStatus(RecommendationStatus.COMPLETED);
    }

    @Override
    @Transactional
    public void deleteRecommendation(Long id) {
        Objects.requireNonNull(id, "id is null");

        ReplenishmentRecommendation recommendation = getRecommendationEntityById(id);
        recommendationRepository.delete(recommendation);
    }

    private ReplenishmentRecommendation getRecommendationEntityById(Long id) {
        Objects.requireNonNull(id, "id is null");

        return recommendationRepository.findById(id)
                .orElseThrow(() -> new ReplenishmentRecommendationIsNotFoundException(
                        "Replenishment recommendation with id " + id + " is not found"));
    }

    private Product getProductEntityById(Long id) {
        Objects.requireNonNull(id, "id is null");

        return productRepository.findById(id)
                .orElseThrow(() -> new ProductIsNotFoundException(
                        "Product with id " + id + " is not found"));
    }

    private ReplenishmentRecommendationResponseDto toResponseDto(
            ReplenishmentRecommendation recommendation) {

        return new ReplenishmentRecommendationResponseDto(
                recommendation.getId(),
                recommendation.getProduct().getId(),
                recommendation.getRecommendedQuantity(),
                recommendation.getRequiredBy(),
                recommendation.getReason(),
                recommendation.getStatus(),
                recommendation.getCreatedAt());
    }
}