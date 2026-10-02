package com.skr1l.backend.stockMovement.service;

import com.skr1l.backend.stockMovement.dto.StockMovementRequestDto;
import com.skr1l.backend.stockMovement.dto.StockMovementResponseDto;
import com.skr1l.backend.stockMovement.enums.StockMovementSource;
import com.skr1l.backend.stockMovement.enums.StockMovementType;
import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementService {

    Long createStockMovement(StockMovementRequestDto dto);

    StockMovementResponseDto getStockMovementById(Long id);

    List<StockMovementResponseDto> getAllStockMovements();

    List<StockMovementResponseDto> getStockMovementsByProductId(Long productId);

    List<StockMovementResponseDto> getStockMovementsByShelfId(Long shelfId);

    List<StockMovementResponseDto> getStockMovementsByType(StockMovementType type);

    List<StockMovementResponseDto> getStockMovementsBySource(StockMovementSource source);

    List<StockMovementResponseDto> getStockMovementsByDateRange(LocalDateTime from, LocalDateTime to);

    void deleteStockMovement(Long id);
}