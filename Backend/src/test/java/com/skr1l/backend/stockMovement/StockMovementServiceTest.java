package com.skr1l.backend.stockMovement;

import com.skr1l.backend.exception.productExceptions.ProductIsNotFoundException;
import com.skr1l.backend.exception.shelfExceptions.ShelfIsNotFoundException;
import com.skr1l.backend.exception.stockMovement.StockMovementIsNotFoundException;
import com.skr1l.backend.product.Product;
import com.skr1l.backend.product.repository.ProductRepository;
import com.skr1l.backend.shelf.Shelf;
import com.skr1l.backend.shelf.repository.ShelfRepository;
import com.skr1l.backend.stockMovement.dto.*;
import com.skr1l.backend.stockMovement.enums.*;
import com.skr1l.backend.stockMovement.repository.StockMovementRepository;
import com.skr1l.backend.stockMovement.service.StockMovementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StockMovementServiceTest {

  @Mock
  private StockMovementRepository repository;

  @Mock
  private ProductRepository productRepository;

  @Mock
  private ShelfRepository shelfRepository;

  @InjectMocks
  private StockMovementServiceImpl service;

  private Product product;
  private Shelf shelf;
  private StockMovement movement;
  private StockMovementRequestDto request;
  private LocalDateTime time;

  @BeforeEach
  public void setup() {
    time = LocalDateTime.of(2026, 10, 15, 12, 0);

    product = new Product();
    product.setId(10L);

    shelf = new Shelf();
    shelf.setId(30L);

    movement = new StockMovement();
    movement.setId(50L);
    movement.setProduct(product);
    movement.setShelf(shelf);
    movement.setQuantityDelta(-2);
    movement.setType(StockMovementType.REMOVAL);
    movement.setSource(StockMovementSource.CAMERA);
    movement.setCreatedAt(time);

    request = new StockMovementRequestDto(
      10L, 30L, -2,
      StockMovementType.REMOVAL,
      StockMovementSource.CAMERA
    );
  }

  @Test
  public void createStockMovementShouldReturnIdAndSaveCorrectFields() {
    //Arrange
    when(productRepository.findById(10L))
      .thenReturn(Optional.of(product));

    when(shelfRepository.findById(30L))
      .thenReturn(Optional.of(shelf));

    when(repository.save(any(StockMovement.class)))
      .thenAnswer(invocation -> {
        StockMovement saved = invocation.getArgument(0);
        saved.setId(50L);
        return saved;
      });

    //Act
    Long result = service.createStockMovement(request);

    //Assert
    assertEquals(50L, result);

    ArgumentCaptor<StockMovement> captor =
      ArgumentCaptor.forClass(StockMovement.class);

    verify(repository).save(captor.capture());

    StockMovement saved = captor.getValue();

    assertSame(product, saved.getProduct());
    assertSame(shelf, saved.getShelf());
    assertEquals(-2, saved.getQuantityDelta());
    assertEquals(StockMovementType.REMOVAL, saved.getType());
    assertEquals(StockMovementSource.CAMERA, saved.getSource());
    assertNotNull(saved.getCreatedAt());
  }

  @Test
  public void createStockMovementShouldThrowWhenProductDoesNotExist() {
    //Arrange
    when(productRepository.findById(10L))
      .thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(ProductIsNotFoundException.class,
      () -> service.createStockMovement(request));

    //Assert
    verifyNoInteractions(shelfRepository);
    verify(repository, never()).save(any(StockMovement.class));
  }

  @Test
  public void createStockMovementShouldThrowWhenShelfDoesNotExist() {
    //Arrange
    when(productRepository.findById(10L))
      .thenReturn(Optional.of(product));

    when(shelfRepository.findById(30L))
      .thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(ShelfIsNotFoundException.class,
      () -> service.createStockMovement(request));

    //Assert
    verify(repository, never()).save(any(StockMovement.class));
  }

  @Test
  public void getStockMovementByIdShouldReturnDto() {
    //Arrange
    when(repository.findById(50L))
      .thenReturn(Optional.of(movement));

    //Act
    StockMovementResponseDto result =
      service.getStockMovementById(50L);

    //Assert
    assertEquals(expectedDto(), result);
  }

  @Test
  public void getStockMovementByIdShouldThrowWhenMovementDoesNotExist() {
    //Arrange
    when(repository.findById(50L)).thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(StockMovementIsNotFoundException.class,
      () -> service.getStockMovementById(50L));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getAllStockMovementsShouldReturnMappedList(boolean empty) {
    //Arrange
    when(repository.findAll())
      .thenReturn(empty ? List.of() : List.of(movement));

    //Act
    List<StockMovementResponseDto> result =
      service.getAllStockMovements();

    //Assert
    assertEquals(empty ? List.of() : List.of(expectedDto()), result);
    verify(repository).findAll();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getStockMovementsByProductIdShouldReturnMappedList(boolean empty) {
    //Arrange
    when(repository.findByProductId(10L))
      .thenReturn(empty ? List.of() : List.of(movement));

    //Act
    List<StockMovementResponseDto> result =
      service.getStockMovementsByProductId(10L);

    //Assert
    assertEquals(empty ? List.of() : List.of(expectedDto()), result);
    verify(repository).findByProductId(10L);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getStockMovementsByShelfIdShouldReturnMappedList(boolean empty) {
    //Arrange
    when(repository.findByShelfId(30L))
      .thenReturn(empty ? List.of() : List.of(movement));

    //Act
    List<StockMovementResponseDto> result =
      service.getStockMovementsByShelfId(30L);

    //Assert
    assertEquals(empty ? List.of() : List.of(expectedDto()), result);
    verify(repository).findByShelfId(30L);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getStockMovementsByTypeShouldReturnMappedList(boolean empty) {
    //Arrange
    when(repository.findByType(StockMovementType.REMOVAL))
      .thenReturn(empty ? List.of() : List.of(movement));

    //Act
    List<StockMovementResponseDto> result =
      service.getStockMovementsByType(StockMovementType.REMOVAL);

    //Assert
    assertEquals(empty ? List.of() : List.of(expectedDto()), result);
    verify(repository).findByType(StockMovementType.REMOVAL);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getStockMovementsBySourceShouldReturnMappedList(boolean empty) {
    //Arrange
    when(repository.findBySource(StockMovementSource.CAMERA))
      .thenReturn(empty ? List.of() : List.of(movement));

    //Act
    List<StockMovementResponseDto> result =
      service.getStockMovementsBySource(StockMovementSource.CAMERA);

    //Assert
    assertEquals(empty ? List.of() : List.of(expectedDto()), result);
    verify(repository).findBySource(StockMovementSource.CAMERA);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getStockMovementsByDateRangeShouldReturnMappedList(boolean empty) {
    //Arrange
    LocalDateTime to = time.plusHours(1);

    when(repository.findByCreatedAtBetween(time, to))
      .thenReturn(empty ? List.of() : List.of(movement));

    //Act
    List<StockMovementResponseDto> result =
      service.getStockMovementsByDateRange(time, to);

    //Assert
    assertEquals(empty ? List.of() : List.of(expectedDto()), result);
    verify(repository).findByCreatedAtBetween(time, to);
  }

  @Test
  public void deleteStockMovementShouldDeleteExistingMovement() {
    //Arrange
    when(repository.findById(50L))
      .thenReturn(Optional.of(movement));

    //Act
    service.deleteStockMovement(50L);

    //Assert
    verify(repository).delete(movement);
  }

  @Test
  public void deleteStockMovementShouldThrowWhenMovementDoesNotExist() {
    //Arrange
    when(repository.findById(50L)).thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(StockMovementIsNotFoundException.class,
      () -> service.deleteStockMovement(50L));

    //Assert
    verify(repository, never()).delete(any(StockMovement.class));
  }

  private StockMovementResponseDto expectedDto() {
    return new StockMovementResponseDto(
      50L, 10L, 30L, -2,
      StockMovementType.REMOVAL,
      StockMovementSource.CAMERA,
      time
    );
  }
}
