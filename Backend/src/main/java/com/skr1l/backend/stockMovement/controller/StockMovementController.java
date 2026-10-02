package com.skr1l.backend.stockMovement.controller;

import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.stockMovement.dto.StockMovementRequestDto;
import com.skr1l.backend.stockMovement.dto.StockMovementResponseDto;
import com.skr1l.backend.stockMovement.enums.StockMovementSource;
import com.skr1l.backend.stockMovement.enums.StockMovementType;
import com.skr1l.backend.stockMovement.service.StockMovementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stock-movements")
public class StockMovementController {

    private static final AppLogger log = LoggerFactory.getLogger(StockMovementController.class);

    private final StockMovementService stockMovementService;

    @PostMapping
    public ResponseEntity<Long> createStockMovement(@Valid @RequestBody StockMovementRequestDto dto) {
        log.info("createStockMovement controller");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stockMovementService.createStockMovement(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockMovementResponseDto> getStockMovementById(@PathVariable Long id) {
        log.info("getStockMovementById controller");

        return ResponseEntity.ok(stockMovementService.getStockMovementById(id));
    }

    @GetMapping
    public ResponseEntity<List<StockMovementResponseDto>> getAllStockMovements() {
        log.info("getAllStockMovements controller");

        return ResponseEntity.ok(stockMovementService.getAllStockMovements());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<StockMovementResponseDto>> getStockMovementsByProductId(
            @PathVariable Long productId) {

        return ResponseEntity.ok(stockMovementService.getStockMovementsByProductId(productId));
    }

    @GetMapping("/shelf/{shelfId}")
    public ResponseEntity<List<StockMovementResponseDto>> getStockMovementsByShelfId(
            @PathVariable Long shelfId) {

        return ResponseEntity.ok(stockMovementService.getStockMovementsByShelfId(shelfId));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<StockMovementResponseDto>> getStockMovementsByType(
            @PathVariable StockMovementType type) {

        return ResponseEntity.ok(stockMovementService.getStockMovementsByType(type));
    }

    @GetMapping("/source/{source}")
    public ResponseEntity<List<StockMovementResponseDto>> getStockMovementsBySource(
            @PathVariable StockMovementSource source) {

        return ResponseEntity.ok(stockMovementService.getStockMovementsBySource(source));
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<StockMovementResponseDto>> getStockMovementsByDateRange(
            @RequestParam LocalDateTime from, @RequestParam LocalDateTime to) {

        return ResponseEntity.ok(stockMovementService.getStockMovementsByDateRange(from, to));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStockMovement(@PathVariable Long id) {
        stockMovementService.deleteStockMovement(id);

        return ResponseEntity.noContent().build();
    }
}