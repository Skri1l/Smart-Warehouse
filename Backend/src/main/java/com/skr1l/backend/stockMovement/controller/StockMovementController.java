package com.skr1l.backend.stockMovement.controller;

import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.stockMovement.dto.StockMovementRequestDto;
import com.skr1l.backend.stockMovement.dto.StockMovementResponseDto;
import com.skr1l.backend.stockMovement.enums.StockMovementSource;
import com.skr1l.backend.stockMovement.enums.StockMovementType;
import com.skr1l.backend.stockMovement.service.StockMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Stock Movements", description = "Stock movement management API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/stock-movements")
public class StockMovementController {

    private static final AppLogger log = LoggerFactory.getLogger(StockMovementController.class);

    private final StockMovementService stockMovementService;

    @Operation(summary = "Create a stock movement")
    @ApiResponse(responseCode = "201", description = "Stock movement created")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "404", description = "Product or shelf not found")
    @PostMapping
    public ResponseEntity<Long> createStockMovement(@Valid @RequestBody StockMovementRequestDto dto) {
        log.info("createStockMovement controller");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stockMovementService.createStockMovement(dto));
    }

    @Operation(summary = "Get stock movement by id")
    @ApiResponse(responseCode = "200", description = "Stock movement found")
    @ApiResponse(responseCode = "404", description = "Stock movement not found")
    @GetMapping("/{id}")
    public ResponseEntity<StockMovementResponseDto> getStockMovementById(@PathVariable Long id) {
        log.info("getStockMovementById controller");

        return ResponseEntity.ok(stockMovementService.getStockMovementById(id));
    }

    @Operation(summary = "Get all stock movements")
    @ApiResponse(responseCode = "200", description = "Stock movements returned successfully")
    @GetMapping
    public ResponseEntity<List<StockMovementResponseDto>> getAllStockMovements() {
        log.info("getAllStockMovements controller");

        return ResponseEntity.ok(stockMovementService.getAllStockMovements());
    }

    @Operation(summary = "Get stock movements by product id")
    @ApiResponse(responseCode = "200", description = "Stock movements returned successfully")
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<StockMovementResponseDto>> getStockMovementsByProductId(
            @PathVariable Long productId) {

        return ResponseEntity.ok(stockMovementService.getStockMovementsByProductId(productId));
    }

    @Operation(summary = "Get stock movements by shelf id")
    @ApiResponse(responseCode = "200", description = "Stock movements returned successfully")
    @GetMapping("/shelf/{shelfId}")
    public ResponseEntity<List<StockMovementResponseDto>> getStockMovementsByShelfId(
            @PathVariable Long shelfId) {

        return ResponseEntity.ok(stockMovementService.getStockMovementsByShelfId(shelfId));
    }

    @Operation(summary = "Get stock movements by type")
    @ApiResponse(responseCode = "200", description = "Stock movements returned successfully")
    @GetMapping("/type/{type}")
    public ResponseEntity<List<StockMovementResponseDto>> getStockMovementsByType(
            @PathVariable StockMovementType type) {

        return ResponseEntity.ok(stockMovementService.getStockMovementsByType(type));
    }

    @Operation(summary = "Get stock movements by source")
    @ApiResponse(responseCode = "200", description = "Stock movements returned successfully")
    @GetMapping("/source/{source}")
    public ResponseEntity<List<StockMovementResponseDto>> getStockMovementsBySource(
            @PathVariable StockMovementSource source) {

        return ResponseEntity.ok(stockMovementService.getStockMovementsBySource(source));
    }

    @Operation(summary = "Get stock movements by date range")
    @ApiResponse(responseCode = "200", description = "Stock movements returned successfully")
    @GetMapping("/date-range")
    public ResponseEntity<List<StockMovementResponseDto>> getStockMovementsByDateRange(
            @RequestParam LocalDateTime from, @RequestParam LocalDateTime to) {

        return ResponseEntity.ok(stockMovementService.getStockMovementsByDateRange(from, to));
    }

    @Operation(summary = "Delete a stock movement")
    @ApiResponse(responseCode = "204", description = "Stock movement deleted")
    @ApiResponse(responseCode = "404", description = "Stock movement not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStockMovement(@PathVariable Long id) {
        stockMovementService.deleteStockMovement(id);

        return ResponseEntity.noContent().build();
    }
}