package com.skr1l.backend.replenishmentRecommendation.controller;

import com.skr1l.backend.replenishmentRecommendation.dto.ReplenishmentRecommendationRequestDto;
import com.skr1l.backend.replenishmentRecommendation.dto.ReplenishmentRecommendationResponseDto;
import com.skr1l.backend.replenishmentRecommendation.enums.RecommendationStatus;
import com.skr1l.backend.replenishmentRecommendation.service.ReplenishmentRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/replenishment-recommendations")
public class ReplenishmentRecommendationController {

    private final ReplenishmentRecommendationService recommendationService;

    @PostMapping
    public ResponseEntity<Long> createRecommendation(
            @Valid @RequestBody ReplenishmentRecommendationRequestDto dto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recommendationService.createRecommendation(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReplenishmentRecommendationResponseDto> getRecommendationById(
            @PathVariable Long id) {

        return ResponseEntity.ok(recommendationService.getRecommendationById(id));
    }

    @GetMapping
    public ResponseEntity<List<ReplenishmentRecommendationResponseDto>> getAllRecommendations() {
        return ResponseEntity.ok(recommendationService.getAllRecommendations());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReplenishmentRecommendationResponseDto>> getRecommendationsByProductId(
            @PathVariable Long productId) {

        return ResponseEntity.ok(recommendationService.getRecommendationsByProductId(productId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ReplenishmentRecommendationResponseDto>> getRecommendationsByStatus(
            @PathVariable RecommendationStatus status) {

        return ResponseEntity.ok(recommendationService.getRecommendationsByStatus(status));
    }

    @GetMapping("/date/{requiredBy}")
    public ResponseEntity<List<ReplenishmentRecommendationResponseDto>> getRecommendationsByRequiredBy(
            @PathVariable LocalDate requiredBy) {

        return ResponseEntity.ok(recommendationService.getRecommendationsByRequiredBy(requiredBy));
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<ReplenishmentRecommendationResponseDto>> getRecommendationsByDateRange(
            @RequestParam LocalDate from, @RequestParam LocalDate to) {

        return ResponseEntity.ok(recommendationService.getRecommendationsByDateRange(from, to));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReplenishmentRecommendationResponseDto> updateRecommendation(
            @PathVariable Long id, @Valid @RequestBody ReplenishmentRecommendationRequestDto dto) {

        return ResponseEntity.ok(recommendationService.updateRecommendation(id, dto));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<Void> approveRecommendation(@PathVariable Long id) {
        recommendationService.approveRecommendation(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<Void> rejectRecommendation(@PathVariable Long id) {
        recommendationService.rejectRecommendation(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ordered")
    public ResponseEntity<Void> markRecommendationAsOrdered(@PathVariable Long id) {
        recommendationService.markRecommendationAsOrdered(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Void> completeRecommendation(@PathVariable Long id) {
        recommendationService.completeRecommendation(id);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecommendation(@PathVariable Long id) {
        recommendationService.deleteRecommendation(id);

        return ResponseEntity.noContent().build();
    }
}