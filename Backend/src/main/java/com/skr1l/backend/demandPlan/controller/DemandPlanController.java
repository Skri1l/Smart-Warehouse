package com.skr1l.backend.demandPlan.controller;

import com.skr1l.backend.demandPlan.dto.DemandPlanRequestDto;
import com.skr1l.backend.demandPlan.dto.DemandPlanResponseDto;
import com.skr1l.backend.demandPlan.service.DemandPlanService;
import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
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

@Tag(name = "Demand Plans", description = "Deman plan management API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/demand-plans")
public class DemandPlanController {

    private static final AppLogger log = LoggerFactory.getLogger(DemandPlanController.class);

    private final DemandPlanService demandPlanService;

    @Operation(summary = "Create a demand plan")
    @ApiResponse(responseCode = "201", description = "Demand plan created")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @PostMapping
    public ResponseEntity<Long> createDemandPlan(@Valid @RequestBody DemandPlanRequestDto dto) {
        log.info("createDemandPlan controller");

        return ResponseEntity.status(HttpStatus.CREATED).body(demandPlanService.createDemandPlan(dto));
    }

    @Operation(summary = "Get demand plan by id")
    @ApiResponse(responseCode = "200", description = "Demand plan found")
    @ApiResponse(responseCode = "404", description = "Demand plan not found")
    @GetMapping("/{id}")
    public ResponseEntity<DemandPlanResponseDto> getDemandPlanById(@PathVariable Long id) {

        return ResponseEntity.ok(demandPlanService.getDemandPlanById(id));
    }

    @Operation(summary = "Get all demand plans")
    @GetMapping
    public ResponseEntity<List<DemandPlanResponseDto>> getAllDemandPlans() {

        return ResponseEntity.ok(demandPlanService.getAllDemandPlans());
    }

    @Operation(summary = "Get demand plans by product id")
    @ApiResponse(responseCode = "200", description = "All plans returned successfully")
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<DemandPlanResponseDto>> getDemandPlansByProductId(
            @PathVariable Long productId) {

        return ResponseEntity.ok(demandPlanService.getDemandPlansByProductId(productId));
    }

    @Operation(summary = "Get demand plans by required date")
    @ApiResponse(responseCode = "200", description = "Plan found")
    @ApiResponse(responseCode = "404", description = "Plan not found")
    @GetMapping("/date/{requiredDate}")
    public ResponseEntity<List<DemandPlanResponseDto>> getDemandPlansByRequiredDate(
            @PathVariable LocalDate requiredDate) {

        return ResponseEntity.ok(demandPlanService.getDemandPlansByRequiredDate(requiredDate));
    }

    @Operation(summary = "Get demand plans by date range")
    @ApiResponse(responseCode = "200", description = "Plan found")
    @ApiResponse(responseCode = "404", description = "Plan not found")
    @GetMapping("/date-range")
    public ResponseEntity<List<DemandPlanResponseDto>> getDemandPlansByDateRange(
            @RequestParam LocalDate from, @RequestParam LocalDate to) {

        return ResponseEntity.ok(demandPlanService.getDemandPlansByDateRange(from, to));
    }

    @Operation(summary = "Get future demand plans")
    @ApiResponse(responseCode = "200", description = "Plan found")
    @ApiResponse(responseCode = "404", description = "Plan not found")
    @GetMapping("/future")
    public ResponseEntity<List<DemandPlanResponseDto>> getFutureDemandPlans() {
        return ResponseEntity.ok(demandPlanService.getFutureDemandPlans());
    }

    @Operation(summary = "Update demand plans")
    @ApiResponse(responseCode = "200", description = "Plan updated")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "404", description = "Plan not found")
    @PutMapping("/{id}")
    public ResponseEntity<DemandPlanResponseDto> updateDemandPlan(
            @PathVariable Long id, @Valid @RequestBody DemandPlanRequestDto dto) {

        return ResponseEntity.ok(demandPlanService.updateDemandPlan(id, dto));
    }

    @Operation(summary = "Delete demand plans")
    @ApiResponse(responseCode = "204", description = "Plan deleted")
    @ApiResponse(responseCode = "404", description = "Plan not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDemandPlan(@PathVariable Long id) {
        demandPlanService.deleteDemandPlan(id);

        return ResponseEntity.noContent().build();
    }
}