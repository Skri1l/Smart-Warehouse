package com.skr1l.backend.stockMovement;

import com.skr1l.backend.stockMovement.controller.StockMovementController;
import com.skr1l.backend.stockMovement.dto.StockMovementRequestDto;
import com.skr1l.backend.stockMovement.dto.StockMovementResponseDto;
import com.skr1l.backend.stockMovement.enums.StockMovementSource;
import com.skr1l.backend.stockMovement.enums.StockMovementType;
import com.skr1l.backend.stockMovement.service.StockMovementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockMovementControllerTest {

  @Mock
  private StockMovementService stockMovementService;

  @InjectMocks
  private StockMovementController controller;

  @Test
  void createStockMovement_shouldReturnCreatedAndId() {
    StockMovementRequestDto request = mock(StockMovementRequestDto.class);
    Long expectedId = 1L;

    when(stockMovementService.createStockMovement(request))
      .thenReturn(expectedId);

    ResponseEntity<Long> response =
      controller.createStockMovement(request);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(expectedId, response.getBody());
    verify(stockMovementService).createStockMovement(request);
  }

  @Test
  void getStockMovementById_shouldReturnOkAndMovement() {
    Long id = 1L;
    StockMovementResponseDto expected =
      mock(StockMovementResponseDto.class);

    when(stockMovementService.getStockMovementById(id))
      .thenReturn(expected);

    ResponseEntity<StockMovementResponseDto> response =
      controller.getStockMovementById(id);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertSame(expected, response.getBody());
    verify(stockMovementService).getStockMovementById(id);
  }

  @Test
  void getAllStockMovements_shouldReturnOkAndMovements() {
    List<StockMovementResponseDto> expected =
      List.of(mock(StockMovementResponseDto.class));

    when(stockMovementService.getAllStockMovements())
      .thenReturn(expected);

    ResponseEntity<List<StockMovementResponseDto>> response =
      controller.getAllStockMovements();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expected, response.getBody());
    verify(stockMovementService).getAllStockMovements();
  }

  @Test
  void getStockMovementsByProductId_shouldReturnOkAndMovements() {
    Long productId = 10L;
    List<StockMovementResponseDto> expected =
      List.of(mock(StockMovementResponseDto.class));

    when(stockMovementService.getStockMovementsByProductId(productId))
      .thenReturn(expected);

    ResponseEntity<List<StockMovementResponseDto>> response =
      controller.getStockMovementsByProductId(productId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expected, response.getBody());
    verify(stockMovementService)
      .getStockMovementsByProductId(productId);
  }

  @Test
  void getStockMovementsByShelfId_shouldReturnOkAndMovements() {
    Long shelfId = 20L;
    List<StockMovementResponseDto> expected =
      List.of(mock(StockMovementResponseDto.class));

    when(stockMovementService.getStockMovementsByShelfId(shelfId))
      .thenReturn(expected);

    ResponseEntity<List<StockMovementResponseDto>> response =
      controller.getStockMovementsByShelfId(shelfId);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expected, response.getBody());
    verify(stockMovementService).getStockMovementsByShelfId(shelfId);
  }

  @Test
  void getStockMovementsByType_shouldReturnOkAndMovements() {
    StockMovementType type = StockMovementType.values()[0];
    List<StockMovementResponseDto> expected =
      List.of(mock(StockMovementResponseDto.class));

    when(stockMovementService.getStockMovementsByType(type))
      .thenReturn(expected);

    ResponseEntity<List<StockMovementResponseDto>> response =
      controller.getStockMovementsByType(type);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expected, response.getBody());
    verify(stockMovementService).getStockMovementsByType(type);
  }

  @Test
  void getStockMovementsBySource_shouldReturnOkAndMovements() {
    StockMovementSource source = StockMovementSource.values()[0];
    List<StockMovementResponseDto> expected =
      List.of(mock(StockMovementResponseDto.class));

    when(stockMovementService.getStockMovementsBySource(source))
      .thenReturn(expected);

    ResponseEntity<List<StockMovementResponseDto>> response =
      controller.getStockMovementsBySource(source);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expected, response.getBody());
    verify(stockMovementService).getStockMovementsBySource(source);
  }

  @Test
  void getStockMovementsByDateRange_shouldReturnOkAndMovements() {
    LocalDateTime from = LocalDateTime.of(2026, 10, 1, 0, 0);
    LocalDateTime to = LocalDateTime.of(2026, 10, 15, 23, 59);
    List<StockMovementResponseDto> expected =
      List.of(mock(StockMovementResponseDto.class));

    when(stockMovementService.getStockMovementsByDateRange(from, to))
      .thenReturn(expected);

    ResponseEntity<List<StockMovementResponseDto>> response =
      controller.getStockMovementsByDateRange(from, to);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(expected, response.getBody());
    verify(stockMovementService)
      .getStockMovementsByDateRange(from, to);
  }

  @Test
  void deleteStockMovement_shouldReturnNoContent() {
    Long id = 1L;

    ResponseEntity<Void> response =
      controller.deleteStockMovement(id);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    assertNull(response.getBody());
    verify(stockMovementService).deleteStockMovement(id);
  }
}
