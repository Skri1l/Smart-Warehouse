package com.skr1l.backend.shelfStock.controller;

import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.shelfStock.dto.ShelfStockRequestDto;
import com.skr1l.backend.shelfStock.dto.ShelfStockResponseDto;
import com.skr1l.backend.shelfStock.service.ShelfStockService;
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

@Tag(name = "Shelf Stock", description = "Shelf stock management API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/shelf-stock")
public class ShelfStockController {

    private static final AppLogger log = LoggerFactory.getLogger(ShelfStockController.class);

    private final ShelfStockService shelfStockService;

    @Operation(summary = "Create shelf stock")
    @ApiResponse(responseCode = "201", description = "Shelf stock created")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "404", description = "Shelf or product not found")
    @PostMapping
    public ResponseEntity<Long> createShelfStock(@Valid @RequestBody ShelfStockRequestDto dto) {
        log.info("createShelfStock controller");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shelfStockService.createShelfStock(dto));
    }

    @Operation(summary = "Get shelf stock by id")
    @ApiResponse(responseCode = "200", description = "Shelf stock found")
    @ApiResponse(responseCode = "404", description = "Shelf stock not found")
    @GetMapping("/{id}")
    public ResponseEntity<ShelfStockResponseDto> getShelfStockById(@PathVariable Long id) {
        log.info("getShelfStockById controller");

        return ResponseEntity.ok(shelfStockService.getShelfStockById(id));
    }

    @Operation(summary = "Get all shelf stock")
    @ApiResponse(responseCode = "200", description = "Shelf stock returned successfully")
    @GetMapping
    public ResponseEntity<List<ShelfStockResponseDto>> getAllShelfStock() {
        log.info("getAllShelfStock controller");

        return ResponseEntity.ok(shelfStockService.getAllShelfStock());
    }

    @Operation(summary = "Get shelf stock by shelf id")
    @ApiResponse(responseCode = "200", description = "Shelf stock returned successfully")
    @GetMapping("/shelf/{shelfId}")
    public ResponseEntity<List<ShelfStockResponseDto>> getShelfStockByShelfId(@PathVariable Long shelfId) {
        log.info("getShelfStockByShelfId controller");

        return ResponseEntity.ok(shelfStockService.getShelfStockByShelfId(shelfId));
    }

    @Operation(summary = "Get shelf stock by product id")
    @ApiResponse(responseCode = "200", description = "Shelf stock returned successfully")
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ShelfStockResponseDto>> getShelfStockByProductId(@PathVariable Long productId) {
        log.info("getShelfStockByProductId controller");

        return ResponseEntity.ok(shelfStockService.getShelfStockByProductId(productId));
    }

    @Operation(summary = "Get shelf stock by expiration date")
    @ApiResponse(responseCode = "200", description = "Shelf stock returned successfully")
    @GetMapping("/expiration-date")
    public ResponseEntity<List<ShelfStockResponseDto>> getShelfStockByExpirationDate(@RequestParam LocalDate date) {
        log.info("getShelfStockByExpirationDate controller");

        return ResponseEntity.ok(shelfStockService.getShelfStockByExpirationDate(date));
    }

    @Operation(summary = "Get expired shelf stock")
    @ApiResponse(responseCode = "200", description = "Expired shelf stock returned successfully")
    @GetMapping("/expired")
    public ResponseEntity<List<ShelfStockResponseDto>> getExpiredShelfStock() {
        log.info("getExpiredShelfStock controller");

        return ResponseEntity.ok(shelfStockService.getExpiredShelfStock());
    }

    @Operation(summary = "Update shelf stock")
    @ApiResponse(responseCode = "200", description = "Shelf stock updated")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "404", description = "Shelf stock not found")
    @PutMapping("/{id}")
    public ResponseEntity<ShelfStockResponseDto> updateShelfStock(@PathVariable Long id, @Valid @RequestBody ShelfStockRequestDto dto) {
        log.info("updateShelfStock controller");

        return ResponseEntity.ok(shelfStockService.updateShelfStock(id, dto));
    }

    @Operation(summary = "Delete shelf stock")
    @ApiResponse(responseCode = "204", description = "Shelf stock deleted")
    @ApiResponse(responseCode = "404", description = "Shelf stock not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShelfStock(@PathVariable Long id) {
        log.info("deleteShelfStock controller");

        shelfStockService.deleteShelfStock(id);

        return ResponseEntity.noContent().build();
    }
}