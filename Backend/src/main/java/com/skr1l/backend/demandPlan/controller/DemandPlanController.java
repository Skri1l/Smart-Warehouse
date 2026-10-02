package com.skr1l.backend.demandPlan.controller;

import com.skr1l.backend.demandPlan.dto.DemandPlanRequestDto;
import com.skr1l.backend.demandPlan.dto.DemandPlanResponseDto;
import com.skr1l.backend.demandPlan.service.DemandPlanService;
import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/demand-plans")
public class DemandPlanController {

    private static final AppLogger log = LoggerFactory.getLogger(DemandPlanController.class);

    private final DemandPlanService demandPlanService;

    @PostMapping
    public ResponseEntity<Long> createDemandPlan(@Valid @RequestBody DemandPlanRequestDto dto) {
        log.info("createDemandPlan controller");

        return ResponseEntity.status(HttpStatus.CREATED).body(demandPlanService.createDemandPlan(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DemandPlanResponseDto> getDemandPlanById(@PathVariable Long id) {

        return ResponseEntity.ok(demandPlanService.getDemandPlanById(id));
    }

    @GetMapping
    public ResponseEntity<List<DemandPlanResponseDto>> getAllDemandPlans() {

        return ResponseEntity.ok(demandPlanService.getAllDemandPlans());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<DemandPlanResponseDto>> getDemandPlansByProductId(
            @PathVariable Long productId) {

        return ResponseEntity.ok(demandPlanService.getDemandPlansByProductId(productId));
    }

    @GetMapping("/date/{requiredDate}")
    public ResponseEntity<List<DemandPlanResponseDto>> getDemandPlansByRequiredDate(
            @PathVariable LocalDate requiredDate) {

        return ResponseEntity.ok(demandPlanService.getDemandPlansByRequiredDate(requiredDate));
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<DemandPlanResponseDto>> getDemandPlansByDateRange(
            @RequestParam LocalDate from, @RequestParam LocalDate to) {

        return ResponseEntity.ok(demandPlanService.getDemandPlansByDateRange(from, to));
    }

    @GetMapping("/future")
    public ResponseEntity<List<DemandPlanResponseDto>> getFutureDemandPlans() {
        return ResponseEntity.ok(demandPlanService.getFutureDemandPlans());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DemandPlanResponseDto> updateDemandPlan(
            @PathVariable Long id, @Valid @RequestBody DemandPlanRequestDto dto) {

        return ResponseEntity.ok(demandPlanService.updateDemandPlan(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDemandPlan(@PathVariable Long id) {
        demandPlanService.deleteDemandPlan(id);

        return ResponseEntity.noContent().build();
    }
}