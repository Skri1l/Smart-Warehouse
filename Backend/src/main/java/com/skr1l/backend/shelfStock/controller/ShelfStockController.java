package com.skr1l.backend.shelfStock.controller;

import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.shelfStock.dto.ShelfStockRequestDto;
import com.skr1l.backend.shelfStock.dto.ShelfStockResponseDto;
import com.skr1l.backend.shelfStock.service.ShelfStockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/shelf-stock")
public class ShelfStockController {

    private static final AppLogger log = LoggerFactory.getLogger(ShelfStockController.class);

    private final ShelfStockService shelfStockService;

    @PostMapping
    public ResponseEntity<Long> createShelfStock(@Valid @RequestBody ShelfStockRequestDto dto) {
        log.info("createShelfStock controller");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shelfStockService.createShelfStock(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShelfStockResponseDto> getShelfStockById(@PathVariable Long id) {
        log.info("getShelfStockById controller");

        return ResponseEntity.ok(shelfStockService.getShelfStockById(id));
    }

    @GetMapping
    public ResponseEntity<List<ShelfStockResponseDto>> getAllShelfStock() {
        log.info("getAllShelfStock controller");

        return ResponseEntity.ok(shelfStockService.getAllShelfStock());
    }

    @GetMapping("/shelf/{shelfId}")
    public ResponseEntity<List<ShelfStockResponseDto>> getShelfStockByShelfId(@PathVariable Long shelfId) {
        log.info("getShelfStockByShelfId controller");

        return ResponseEntity.ok(shelfStockService.getShelfStockByShelfId(shelfId));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ShelfStockResponseDto>> getShelfStockByProductId(
            @PathVariable Long productId) {
        log.info("getShelfStockByProductId controller");

        return ResponseEntity.ok(shelfStockService.getShelfStockByProductId(productId));
    }

    @GetMapping("/expiration-date")
    public ResponseEntity<List<ShelfStockResponseDto>> getShelfStockByExpirationDate(
            @RequestParam LocalDate date) {
        log.info("getShelfStockByExpirationDate controller");

        return ResponseEntity.ok(shelfStockService.getShelfStockByExpirationDate(date));
    }

    @GetMapping("/expired")
    public ResponseEntity<List<ShelfStockResponseDto>> getExpiredShelfStock() {
        log.info("getExpiredShelfStock controller");

        return ResponseEntity.ok(shelfStockService.getExpiredShelfStock());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShelfStockResponseDto> updateShelfStock(@PathVariable Long id,
            @Valid @RequestBody ShelfStockRequestDto dto) {
        log.info("updateShelfStock controller");

        return ResponseEntity.ok(shelfStockService.updateShelfStock(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShelfStock(@PathVariable Long id) {
        log.info("deleteShelfStock controller");

        shelfStockService.deleteShelfStock(id);

        return ResponseEntity.noContent().build();
    }
}