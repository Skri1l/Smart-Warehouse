package com.skr1l.backend.recognitionResult;

import com.skr1l.backend.exception.imageEvent.ImageEventIsNotFoundException;
import com.skr1l.backend.exception.productExceptions.ProductIsNotFoundException;
import com.skr1l.backend.exception.recognitionResult.RecognitionResultIsNotFoundException;
import com.skr1l.backend.imageEvent.ImageEvent;
import com.skr1l.backend.imageEvent.repository.ImageEventRepository;
import com.skr1l.backend.product.Product;
import com.skr1l.backend.product.repository.ProductRepository;
import com.skr1l.backend.recognitionResult.dto.*;
import com.skr1l.backend.recognitionResult.enums.RecognitionStatus;
import com.skr1l.backend.recognitionResult.repository.RecognitionResultRepository;
import com.skr1l.backend.recognitionResult.service.RecognitionResultServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecognitionResultServiceTest {

  @Mock
  private RecognitionResultRepository repository;

  @Mock
  private ImageEventRepository imageEventRepository;

  @Mock
  private ProductRepository productRepository;

  @InjectMocks
  private RecognitionResultServiceImpl service;

  private Product product;
  private ImageEvent imageEvent;
  private RecognitionResult recognition;
  private RecognitionResultRequestDto request;

  @BeforeEach
  public void setup() {
    product = new Product();
    product.setId(10L);

    imageEvent = new ImageEvent();
    imageEvent.setId(40L);

    recognition = new RecognitionResult();
    recognition.setId(50L);
    recognition.setProduct(product);
    recognition.setImageEvent(imageEvent);
    recognition.setDetectedQuantity(14);
    recognition.setQuantityDelta(-2);
    recognition.setConfidence(0.95);
    recognition.setStatus(RecognitionStatus.DETECTED);

    request = new RecognitionResultRequestDto(40L, 10L, 14, -2, 0.95);
  }

  @Test
  public void createRecognitionResultShouldReturnIdAndSaveCorrectFields() {
    //Arrange
    when(imageEventRepository.findById(40L))
      .thenReturn(Optional.of(imageEvent));

    when(productRepository.findById(10L))
      .thenReturn(Optional.of(product));

    when(repository.save(any(RecognitionResult.class)))
      .thenAnswer(invocation -> {
        RecognitionResult saved = invocation.getArgument(0);
        saved.setId(50L);
        return saved;
      });

    //Act
    Long result = service.createRecognitionResult(request);

    //Assert
    assertEquals(50L, result);

    ArgumentCaptor<RecognitionResult> captor =
      ArgumentCaptor.forClass(RecognitionResult.class);

    verify(repository).save(captor.capture());

    RecognitionResult saved = captor.getValue();

    assertSame(imageEvent, saved.getImageEvent());
    assertSame(product, saved.getProduct());
    assertEquals(14, saved.getDetectedQuantity());
    assertEquals(-2, saved.getQuantityDelta());
    assertEquals(0.95, saved.getConfidence());
    assertEquals(RecognitionStatus.DETECTED, saved.getStatus());
  }

  @Test
  public void createRecognitionResultShouldThrowWhenImageEventDoesNotExist() {
    //Arrange
    when(imageEventRepository.findById(40L))
      .thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(ImageEventIsNotFoundException.class,
      () -> service.createRecognitionResult(request));

    //Assert
    verifyNoInteractions(productRepository);
    verify(repository, never()).save(any(RecognitionResult.class));
  }

  @Test
  public void createRecognitionResultShouldThrowWhenProductDoesNotExist() {
    //Arrange
    when(imageEventRepository.findById(40L))
      .thenReturn(Optional.of(imageEvent));

    when(productRepository.findById(10L))
      .thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(ProductIsNotFoundException.class,
      () -> service.createRecognitionResult(request));

    //Assert
    verify(repository, never()).save(any(RecognitionResult.class));
  }

  @Test
  public void getRecognitionResultByIdShouldReturnDto() {
    //Arrange
    when(repository.findById(50L))
      .thenReturn(Optional.of(recognition));

    //Act
    RecognitionResultResponseDto result =
      service.getRecognitionResultById(50L);

    //Assert
    assertEquals(expectedDto(), result);
  }

  @Test
  public void getRecognitionResultByIdShouldThrowWhenResultDoesNotExist() {
    //Arrange
    when(repository.findById(50L)).thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(RecognitionResultIsNotFoundException.class,
      () -> service.getRecognitionResultById(50L));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getAllRecognitionResultsShouldReturnMappedList(boolean empty) {
    //Arrange
    when(repository.findAll())
      .thenReturn(empty ? List.of() : List.of(recognition));

    //Act
    List<RecognitionResultResponseDto> result =
      service.getAllRecognitionResults();

    //Assert
    assertEquals(empty ? List.of() : List.of(expectedDto()), result);
    verify(repository).findAll();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getRecognitionResultsByImageEventIdShouldReturnMappedList(
    boolean empty
  ) {
    //Arrange
    when(repository.findByImageEventId(40L))
      .thenReturn(empty ? List.of() : List.of(recognition));

    //Act
    List<RecognitionResultResponseDto> result =
      service.getRecognitionResultsByImageEventId(40L);

    //Assert
    assertEquals(empty ? List.of() : List.of(expectedDto()), result);
    verify(repository).findByImageEventId(40L);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getRecognitionResultsByProductIdShouldReturnMappedList(
    boolean empty
  ) {
    //Arrange
    when(repository.findByProductId(10L))
      .thenReturn(empty ? List.of() : List.of(recognition));

    //Act
    List<RecognitionResultResponseDto> result =
      service.getRecognitionResultsByProductId(10L);

    //Assert
    assertEquals(empty ? List.of() : List.of(expectedDto()), result);
    verify(repository).findByProductId(10L);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getRecognitionResultsByStatusShouldReturnMappedList(boolean empty) {
    //Arrange
    when(repository.findByStatus(RecognitionStatus.DETECTED))
      .thenReturn(empty ? List.of() : List.of(recognition));

    //Act
    List<RecognitionResultResponseDto> result =
      service.getRecognitionResultsByStatus(RecognitionStatus.DETECTED);

    //Assert
    assertEquals(empty ? List.of() : List.of(expectedDto()), result);
    verify(repository).findByStatus(RecognitionStatus.DETECTED);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getRecognitionResultsByMinConfidenceShouldReturnMappedList(
    boolean empty
  ) {
    //Arrange
    when(repository.findByConfidenceGreaterThanEqual(0.9))
      .thenReturn(empty ? List.of() : List.of(recognition));

    //Act
    List<RecognitionResultResponseDto> result =
      service.getRecognitionResultsByMinConfidence(0.9);

    //Assert
    assertEquals(empty ? List.of() : List.of(expectedDto()), result);
    verify(repository).findByConfidenceGreaterThanEqual(0.9);
  }

  @ParameterizedTest
  @ValueSource(strings = {"confirm", "reject", "failed"})
  public void statusMethodsShouldChangeStatus(String action) {
    //Arrange
    when(repository.findById(50L))
      .thenReturn(Optional.of(recognition));

    //Act
    changeStatus(action);

    //Assert
    RecognitionStatus expected = switch (action) {
      case "confirm" -> RecognitionStatus.CONFIRMED;
      case "reject" -> RecognitionStatus.REJECTED;
      default -> RecognitionStatus.FAILED;
    };

    assertEquals(expected, recognition.getStatus());
  }

  @ParameterizedTest
  @ValueSource(strings = {"confirm", "reject", "failed"})
  public void statusMethodsShouldThrowWhenResultDoesNotExist(String action) {
    //Arrange
    when(repository.findById(50L)).thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(RecognitionResultIsNotFoundException.class,
      () -> changeStatus(action));
  }

  @Test
  public void deleteRecognitionResultShouldDeleteExistingResult() {
    //Arrange
    when(repository.findById(50L))
      .thenReturn(Optional.of(recognition));

    //Act
    service.deleteRecognitionResult(50L);

    //Assert
    verify(repository).delete(recognition);
  }

  @Test
  public void deleteRecognitionResultShouldThrowWhenResultDoesNotExist() {
    //Arrange
    when(repository.findById(50L)).thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(RecognitionResultIsNotFoundException.class,
      () -> service.deleteRecognitionResult(50L));

    //Assert
    verify(repository, never()).delete(any(RecognitionResult.class));
  }

  private void changeStatus(String action) {
    switch (action) {
      case "confirm" -> service.confirmRecognitionResult(50L);
      case "reject" -> service.rejectRecognitionResult(50L);
      case "failed" -> service.markRecognitionResultAsFailed(50L);
      default -> throw new IllegalArgumentException(action);
    }
  }

  private RecognitionResultResponseDto expectedDto() {
    return new RecognitionResultResponseDto(
      50L,
      40L,
      10L,
      14,
      -2,
      0.95,
      RecognitionStatus.DETECTED);
  }
}
