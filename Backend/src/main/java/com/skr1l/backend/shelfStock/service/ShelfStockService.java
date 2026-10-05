package com.skr1l.backend.shelfStock.service;

import com.skr1l.backend.shelfStock.dto.ShelfStockRequestDto;
import com.skr1l.backend.shelfStock.dto.ShelfStockResponseDto;
import java.time.LocalDate;
import java.util.List;

public interface ShelfStockService {

    Long createShelfStock(ShelfStockRequestDto dto);

    ShelfStockResponseDto getShelfStockById(Long id);

    List<ShelfStockResponseDto> getAllShelfStock();

    List<ShelfStockResponseDto> getShelfStockByShelfId(Long shelfId);

    List<ShelfStockResponseDto> getShelfStockByProductId(Long productId);

    List<ShelfStockResponseDto> getShelfStockByExpirationDate(LocalDate expirationDate);

    List<ShelfStockResponseDto> getExpiredShelfStock();

    ShelfStockResponseDto updateShelfStock(Long id, ShelfStockRequestDto dto);

    void deleteShelfStock(Long id);
}