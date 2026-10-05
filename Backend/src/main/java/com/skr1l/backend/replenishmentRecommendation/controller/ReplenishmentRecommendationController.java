package com.skr1l.backend.replenishmentRecommendation.controller;

import com.skr1l.backend.replenishmentRecommendation.dto.ReplenishmentRecommendationRequestDto;
import com.skr1l.backend.replenishmentRecommendation.dto.ReplenishmentRecommendationResponseDto;
import com.skr1l.backend.replenishmentRecommendation.enums.RecommendationStatus;
import com.skr1l.backend.replenishmentRecommendation.service.ReplenishmentRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Replenishment Recommendations", description = "Replenishment recommendation management API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/replenishment-recommendations")
public class ReplenishmentRecommendationController {

    private final ReplenishmentRecommendationService recommendationService;

    @Operation(summary = "Create a replenishment recommendation")
    @ApiResponse(responseCode = "201", description = "Replenishment recommendation created")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "404", description = "Product not found")
    @PostMapping
    public ResponseEntity<Long> createRecommendation(
            @Valid @RequestBody ReplenishmentRecommendationRequestDto dto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recommendationService.createRecommendation(dto));
    }

    @Operation(summary = "Get replenishment recommendation by id")
    @ApiResponse(responseCode = "200", description = "Replenishment recommendation found")
    @ApiResponse(responseCode = "404", description = "Replenishment recommendation not found")
    @GetMapping("/{id}")
    public ResponseEntity<ReplenishmentRecommendationResponseDto> getRecommendationById(
            @PathVariable Long id) {

        return ResponseEntity.ok(recommendationService.getRecommendationById(id));
    }

    @Operation(summary = "Get all replenishment recommendations")
    @ApiResponse(responseCode = "200", description = "Replenishment recommendations returned successfully")
    @GetMapping
    public ResponseEntity<List<ReplenishmentRecommendationResponseDto>> getAllRecommendations() {

        return ResponseEntity.ok(recommendationService
                .getAllRecommendations());
    }

    @Operation(summary = "Get replenishment recommendations by product id")
    @ApiResponse(responseCode = "200", description = "Replenishment recommendations returned successfully")
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReplenishmentRecommendationResponseDto>> getRecommendationsByProductId(
            @PathVariable Long productId) {

        return ResponseEntity.ok(recommendationService
                .getRecommendationsByProductId(productId));
    }

    @Operation(summary = "Get replenishment recommendations by status")
    @ApiResponse(responseCode = "200", description = "Replenishment recommendations returned successfully")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ReplenishmentRecommendationResponseDto>> getRecommendationsByStatus(
            @PathVariable RecommendationStatus status) {

        return ResponseEntity.ok(recommendationService
                .getRecommendationsByStatus(status));
    }

    @Operation(summary = "Get replenishment recommendations by required date")
    @ApiResponse(responseCode = "200", description = "Replenishment recommendations returned successfully")
    @GetMapping("/date/{requiredBy}")
    public ResponseEntity<List<ReplenishmentRecommendationResponseDto>> getRecommendationsByRequiredBy(
            @PathVariable LocalDate requiredBy) {

        return ResponseEntity.ok(recommendationService
                .getRecommendationsByRequiredBy(requiredBy));
    }

    @Operation(summary = "Get replenishment recommendations by date range")
    @ApiResponse(responseCode = "200", description = "Replenishment recommendations returned successfully")
    @GetMapping("/date-range")
    public ResponseEntity<List<ReplenishmentRecommendationResponseDto>> getRecommendationsByDateRange(
            @RequestParam LocalDate from, @RequestParam LocalDate to) {

        return ResponseEntity.ok(recommendationService.getRecommendationsByDateRange(from, to));
    }

    @Operation(summary = "Update a replenishment recommendation")
    @ApiResponse(responseCode = "200", description = "Replenishment recommendation updated")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "404", description = "Replenishment recommendation or product not found")
    @PutMapping("/{id}")
    public ResponseEntity<ReplenishmentRecommendationResponseDto> updateRecommendation(
            @PathVariable Long id, @Valid @RequestBody ReplenishmentRecommendationRequestDto dto) {

        return ResponseEntity
                .ok(recommendationService.updateRecommendation(id, dto));
    }

    @Operation(summary = "Approve a replenishment recommendation")
    @ApiResponse(responseCode = "204", description = "Replenishment recommendation approved")
    @ApiResponse(responseCode = "404", description = "Replenishment recommendation not found")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<Void> approveRecommendation(@PathVariable Long id) {
        recommendationService.approveRecommendation(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reject a replenishment recommendation")
    @ApiResponse(responseCode = "204", description = "Replenishment recommendation rejected")
    @ApiResponse(responseCode = "404", description = "Replenishment recommendation not found")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<Void> rejectRecommendation(@PathVariable Long id) {
        recommendationService.rejectRecommendation(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark replenishment recommendation as ordered")
    @ApiResponse(responseCode = "204", description = "Replenishment recommendation marked as ordered")
    @ApiResponse(responseCode = "404", description = "Replenishment recommendation not found")
    @PatchMapping("/{id}/ordered")
    public ResponseEntity<Void> markRecommendationAsOrdered(@PathVariable Long id) {
        recommendationService.markRecommendationAsOrdered(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Complete a replenishment recommendation")
    @ApiResponse(responseCode = "204", description = "Replenishment recommendation completed")
    @ApiResponse(responseCode = "404", description = "Replenishment recommendation not found")
    @PatchMapping("/{id}/complete")
    public ResponseEntity<Void> completeRecommendation(@PathVariable Long id) {
        recommendationService.completeRecommendation(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete a replenishment recommendation")
    @ApiResponse(responseCode = "204", description = "Replenishment recommendation deleted")
    @ApiResponse(responseCode = "404", description = "Replenishment recommendation not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecommendation(@PathVariable Long id) {
        recommendationService.deleteRecommendation(id);

        return ResponseEntity.noContent().build();
    }
}