package com.skr1l.backend.vision;

import com.skr1l.backend.device.Device;
import com.skr1l.backend.device.enums.DeviceStatus;
import com.skr1l.backend.device.repository.DeviceRepository;
import com.skr1l.backend.exception.deviceExceptions.DeviceIsNotFoundException;
import com.skr1l.backend.imageEvent.ImageEvent;
import com.skr1l.backend.imageEvent.enums.ImageProcessingStatus;
import com.skr1l.backend.imageEvent.repository.ImageEventRepository;
import com.skr1l.backend.product.Product;
import com.skr1l.backend.recognitionResult.RecognitionResult;
import com.skr1l.backend.recognitionResult.enums.RecognitionStatus;
import com.skr1l.backend.recognitionResult.repository.RecognitionResultRepository;
import com.skr1l.backend.shelf.Shelf;
import com.skr1l.backend.shelfStock.ShelfStock;
import com.skr1l.backend.shelfStock.repository.ShelfStockRepository;
import com.skr1l.backend.stockMovement.StockMovement;
import com.skr1l.backend.stockMovement.enums.StockMovementSource;
import com.skr1l.backend.stockMovement.enums.StockMovementType;
import com.skr1l.backend.stockMovement.repository.StockMovementRepository;
import com.skr1l.backend.vision.dto.CameraImageResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CameraStockServiceTest {

  private static final Long DEVICE_ID = 1L;
  private static final Long SHELF_ID = 2L;

  private static final LocalDateTime CAPTURED_AT =
    LocalDateTime.of(2026, 10, 15, 12, 0);

  private static final LocalDateTime RECEIVED_AT =
    CAPTURED_AT.plusSeconds(1);

  private static final String IMAGE_PATH = "/images/test.image";

  @Mock
  private DeviceRepository deviceRepository;

  @Mock
  private ShelfStockRepository shelfStockRepository;

  @Mock
  private ImageEventRepository imageEventRepository;

  @Mock
  private RecognitionResultRepository recognitionResultRepository;

  @Mock
  private StockMovementRepository stockMovementRepository;

  @InjectMocks
  private CameraStockService service;

  private Device device;
  private Shelf shelf;
  private Product product;
  private ShelfStock stock;

  @BeforeEach
  void setUp() {
    shelf = new Shelf();
    shelf.setId(SHELF_ID);

    product = new Product();

    device = new Device();
    device.setStatus(DeviceStatus.ACTIVE);
    device.setShelf(shelf);

    stock = new ShelfStock();
    stock.setShelf(shelf);
    stock.setProduct(product);
    stock.setQuantity(10);
  }

  @ParameterizedTest
  @ValueSource(ints = {15, 4, 0, 10})
  void apply_shouldUpdateStockAndSaveRecognition(int count) {
    prepareValidState();

    // Имитируем присвоение ID при сохранении в БД.
    when(imageEventRepository.save(any(ImageEvent.class)))
      .thenAnswer(invocation -> {
        ImageEvent event = invocation.getArgument(0);
        event.setId(100L);
        return event;
      });

    when(recognitionResultRepository.save(any(RecognitionResult.class)))
      .thenAnswer(invocation -> {
        RecognitionResult result = invocation.getArgument(0);
        result.setId(200L);
        return result;
      });

    int expectedDelta = count - 10;
    LocalDateTime before = LocalDateTime.now();

    CameraImageResponseDto response = apply(count);

    LocalDateTime after = LocalDateTime.now();

    assertThat(response)
      .usingRecursiveComparison()
      .isEqualTo(new CameraImageResponseDto(
        100L,
        200L,
        SHELF_ID,
        10,
        count,
        expectedDelta
      ));

    assertEquals(count, stock.getQuantity());
    assertEquals(RECEIVED_AT, device.getLastSeenAt());

    ArgumentCaptor<ImageEvent> eventCaptor =
      ArgumentCaptor.forClass(ImageEvent.class);

    verify(imageEventRepository).save(eventCaptor.capture());

    ImageEvent event = eventCaptor.getValue();

    assertSame(device, event.getDevice());
    assertSame(shelf, event.getShelf());
    assertEquals(IMAGE_PATH, event.getImagePath());
    assertEquals(CAPTURED_AT, event.getCapturedAt());
    assertEquals(RECEIVED_AT, event.getReceivedAt());
    assertEquals(
      ImageProcessingStatus.PROCESSED,
      event.getImageProcessingStatus()
    );

    ArgumentCaptor<RecognitionResult> resultCaptor =
      ArgumentCaptor.forClass(RecognitionResult.class);

    verify(recognitionResultRepository).save(resultCaptor.capture());

    RecognitionResult result = resultCaptor.getValue();

    assertSame(event, result.getImageEvent());
    assertSame(product, result.getProduct());
    assertEquals(count, result.getDetectedQuantity());
    assertEquals(expectedDelta, result.getQuantityDelta());
    assertNull(result.getConfidence());
    assertEquals(RecognitionStatus.CONFIRMED, result.getStatus());

    if (expectedDelta == 0) {
      verifyNoInteractions(stockMovementRepository);
    } else {
      ArgumentCaptor<StockMovement> movementCaptor =
        ArgumentCaptor.forClass(StockMovement.class);

      verify(stockMovementRepository).save(movementCaptor.capture());

      StockMovement movement = movementCaptor.getValue();

      assertSame(product, movement.getProduct());
      assertSame(shelf, movement.getShelf());
      assertSame(event, movement.getImageEvent());
      assertEquals(expectedDelta, movement.getQuantityDelta());
      assertEquals(StockMovementType.ADJUSTMENT, movement.getType());
      assertEquals(StockMovementSource.CAMERA, movement.getSource());

      assertNotNull(movement.getCreatedAt());
      assertFalse(movement.getCreatedAt().isBefore(before));
      assertFalse(movement.getCreatedAt().isAfter(after));
    }
  }

  @Test
  void apply_shouldRejectNegativeCount() {
    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> apply(-1)
    );

    assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
    assertEquals(
      "Vision service returned a negative count",
      exception.getReason()
    );

    verifyNoInteractions(
      deviceRepository,
      shelfStockRepository,
      imageEventRepository,
      recognitionResultRepository,
      stockMovementRepository
    );
  }

  @Test
  void apply_shouldThrowWhenDeviceDoesNotExist() {
    when(deviceRepository.findById(DEVICE_ID))
      .thenReturn(Optional.empty());

    DeviceIsNotFoundException exception = assertThrows(
      DeviceIsNotFoundException.class,
      () -> apply(15)
    );

    assertEquals(
      "Device not found: " + DEVICE_ID,
      exception.getMessage()
    );

    verifyNoInteractions(
      shelfStockRepository,
      imageEventRepository,
      recognitionResultRepository,
      stockMovementRepository
    );
  }

  @ParameterizedTest
  @EnumSource(
    value = DeviceStatus.class,
    mode = EnumSource.Mode.EXCLUDE,
    names = "ACTIVE"
  )
  void apply_shouldRejectInactiveDevice(DeviceStatus status) {
    device.setStatus(status);

    when(deviceRepository.findById(DEVICE_ID))
      .thenReturn(Optional.of(device));

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> apply(15)
    );

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    assertEquals("Device is inactive", exception.getReason());
    assertNull(device.getLastSeenAt());

    verifyNoInteractions(
      shelfStockRepository,
      imageEventRepository,
      recognitionResultRepository,
      stockMovementRepository
    );
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 2})
  void apply_shouldRejectShelfWithoutExactlyOneStock(int stockCount) {
    when(deviceRepository.findById(DEVICE_ID))
      .thenReturn(Optional.of(device));

    List<ShelfStock> stocks = IntStream.range(0, stockCount)
      .mapToObj(index -> new ShelfStock())
      .toList();

    when(shelfStockRepository.findAllByShelfId(SHELF_ID))
      .thenReturn(stocks);

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> apply(15)
    );

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    assertEquals(
      "Camera processing requires exactly one stock record on the shelf",
      exception.getReason()
    );
    assertNull(device.getLastSeenAt());

    verifyNoInteractions(
      imageEventRepository,
      recognitionResultRepository,
      stockMovementRepository
    );
  }

  @Test
  void apply_shouldRejectAlreadyProcessedOrOlderImage() {
    when(deviceRepository.findById(DEVICE_ID))
      .thenReturn(Optional.of(device));

    when(shelfStockRepository.findAllByShelfId(SHELF_ID))
      .thenReturn(List.of(stock));

    when(imageEventRepository
      .existsByShelfIdAndImageProcessingStatusAndCapturedAtGreaterThanEqual(
        SHELF_ID,
        ImageProcessingStatus.PROCESSED,
        CAPTURED_AT
      )).thenReturn(true);

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> apply(15)
    );

    assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    assertEquals(
      "An image with the same or a later capture time was already processed",
      exception.getReason()
    );

    assertEquals(10, stock.getQuantity());
    assertNull(device.getLastSeenAt());

    verify(imageEventRepository, never()).save(any(ImageEvent.class));
    verifyNoInteractions(
      recognitionResultRepository,
      stockMovementRepository
    );
  }

  private void prepareValidState() {
    when(deviceRepository.findById(DEVICE_ID))
      .thenReturn(Optional.of(device));

    when(shelfStockRepository.findAllByShelfId(SHELF_ID))
      .thenReturn(List.of(stock));

    when(imageEventRepository
      .existsByShelfIdAndImageProcessingStatusAndCapturedAtGreaterThanEqual(
        SHELF_ID,
        ImageProcessingStatus.PROCESSED,
        CAPTURED_AT
      )).thenReturn(false);
  }

  private CameraImageResponseDto apply(int count) {
    return service.apply(
      DEVICE_ID,
      CAPTURED_AT,
      RECEIVED_AT,
      IMAGE_PATH,
      count
    );
  }
}
