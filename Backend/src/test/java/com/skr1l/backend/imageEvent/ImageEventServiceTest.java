package com.skr1l.backend.imageEvent;

import com.skr1l.backend.device.Device;
import com.skr1l.backend.device.repository.DeviceRepository;
import com.skr1l.backend.exception.deviceExceptions.DeviceIsNotFoundException;
import com.skr1l.backend.exception.imageEvent.ImageEventIsNotFoundException;
import com.skr1l.backend.imageEvent.dto.*;
import com.skr1l.backend.imageEvent.enums.ImageProcessingStatus;
import com.skr1l.backend.imageEvent.repository.ImageEventRepository;
import com.skr1l.backend.imageEvent.service.ImageEventServiceImpl;
import com.skr1l.backend.shelf.Shelf;
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
public class ImageEventServiceTest {

  @Mock
  private ImageEventRepository repository;

  @Mock
  private DeviceRepository deviceRepository;

  @InjectMocks
  private ImageEventServiceImpl service;

  private Device device;
  private Shelf shelf;
  private ImageEvent event;
  private ImageEventRequestDto request;
  private LocalDateTime time;

  @BeforeEach
  public void setup() {
    time = LocalDateTime.of(2026, 10, 15, 12, 0);

    shelf = new Shelf();
    shelf.setId(30L);

    device = new Device();
    device.setId(20L);
    device.setShelf(shelf);

    event = new ImageEvent();
    event.setId(40L);
    event.setDevice(device);
    event.setShelf(shelf);
    event.setImagePath("/images/test.jpg");
    event.setCapturedAt(time);
    event.setReceivedAt(time.plusSeconds(1));
    event.setImageProcessingStatus(ImageProcessingStatus.RECEIVED);

    request = new ImageEventRequestDto(20L, "/images/test.jpg", time);
  }

  @Test
  public void createImageEventShouldReturnIdAndSaveCorrectFields() {
    //Arrange
    when(deviceRepository.findById(20L))
      .thenReturn(Optional.of(device));

    when(repository.save(any(ImageEvent.class)))
      .thenAnswer(invocation -> {
        ImageEvent saved = invocation.getArgument(0);
        saved.setId(40L);
        return saved;
      });

    //Act
    Long result = service.createImageEvent(request);

    //Assert
    assertEquals(40L, result);

    ArgumentCaptor<ImageEvent> captor =
      ArgumentCaptor.forClass(ImageEvent.class);

    verify(repository).save(captor.capture());

    ImageEvent saved = captor.getValue();

    assertSame(device, saved.getDevice());
    assertSame(shelf, saved.getShelf());
    assertEquals(request.imagePath(), saved.getImagePath());
    assertEquals(time, saved.getCapturedAt());
    assertNotNull(saved.getReceivedAt());
    assertEquals(
      ImageProcessingStatus.RECEIVED,
      saved.getImageProcessingStatus()
    );
  }

  @Test
  public void createImageEventShouldThrowWhenDeviceDoesNotExist() {
    //Arrange
    when(deviceRepository.findById(20L))
      .thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(DeviceIsNotFoundException.class,
      () -> service.createImageEvent(request));

    //Assert
    verify(repository, never()).save(any(ImageEvent.class));
  }

  @Test
  public void getImageEventByIdShouldReturnDto() {
    //Arrange
    when(repository.findById(40L)).thenReturn(Optional.of(event));

    //Act
    ImageEventResponseDto result = service.getImageEventById(40L);

    //Assert
    assertEquals(expectedDto(), result);
  }

  @Test
  public void getImageEventByIdShouldThrowWhenEventDoesNotExist() {
    //Arrange
    when(repository.findById(40L)).thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(ImageEventIsNotFoundException.class,
      () -> service.getImageEventById(40L));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getAllImageEventsShouldReturnMappedList(boolean empty) {
    //Arrange
    when(repository.findAll())
      .thenReturn(empty ? List.of() : List.of(event));

    //Act
    List<ImageEventResponseDto> result = service.getAllImageEvents();

    //Assert
    assertEquals(empty ? List.of() : List.of(expectedDto()), result);
    verify(repository).findAll();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getImageEventsByDeviceIdShouldReturnMappedList(boolean empty) {
    //Arrange
    when(repository.findByDeviceId(20L))
      .thenReturn(empty ? List.of() : List.of(event));

    //Act
    List<ImageEventResponseDto> result =
      service.getImageEventsByDeviceId(20L);

    //Assert
    assertEquals(empty ? List.of() : List.of(expectedDto()), result);
    verify(repository).findByDeviceId(20L);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getImageEventsByShelfIdShouldReturnMappedList(boolean empty) {
    //Arrange
    when(repository.findByShelfId(30L))
      .thenReturn(empty ? List.of() : List.of(event));

    //Act
    List<ImageEventResponseDto> result =
      service.getImageEventsByShelfId(30L);

    //Assert
    assertEquals(empty ? List.of() : List.of(expectedDto()), result);
    verify(repository).findByShelfId(30L);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getImageEventsByStatusShouldReturnMappedList(boolean empty) {
    //Arrange
    when(repository.findByImageProcessingStatus(ImageProcessingStatus.RECEIVED))
      .thenReturn(empty ? List.of() : List.of(event));

    //Act
    List<ImageEventResponseDto> result =
      service.getImageEventsByStatus(ImageProcessingStatus.RECEIVED);

    //Assert
    assertEquals(empty ? List.of() : List.of(expectedDto()), result);
    verify(repository)
      .findByImageProcessingStatus(ImageProcessingStatus.RECEIVED);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getImageEventsByDateRangeShouldReturnMappedList(boolean empty) {
    //Arrange
    LocalDateTime to = time.plusHours(1);

    when(repository.findByCapturedAtBetween(time, to))
      .thenReturn(empty ? List.of() : List.of(event));

    //Act
    List<ImageEventResponseDto> result =
      service.getImageEventsByDateRange(time, to);

    //Assert
    assertEquals(empty ? List.of() : List.of(expectedDto()), result);
    verify(repository).findByCapturedAtBetween(time, to);
  }

  @ParameterizedTest
  @ValueSource(strings = {"processing", "processed", "failed"})
  public void statusMethodsShouldChangeStatus(String action) {
    //Arrange
    when(repository.findById(40L)).thenReturn(Optional.of(event));

    //Act
    changeStatus(action);

    //Assert
    ImageProcessingStatus expected = switch (action) {
      case "processing" -> ImageProcessingStatus.PROCESSING;
      case "processed" -> ImageProcessingStatus.PROCESSED;
      default -> ImageProcessingStatus.FAILED;
    };

    assertEquals(expected, event.getImageProcessingStatus());
  }

  @ParameterizedTest
  @ValueSource(strings = {"processing", "processed", "failed"})
  public void statusMethodsShouldThrowWhenEventDoesNotExist(String action) {
    //Arrange
    when(repository.findById(40L)).thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(ImageEventIsNotFoundException.class,
      () -> changeStatus(action));
  }

  @Test
  public void deleteImageEventShouldDeleteExistingEvent() {
    //Arrange
    when(repository.findById(40L)).thenReturn(Optional.of(event));

    //Act
    service.deleteImageEvent(40L);

    //Assert
    verify(repository).delete(event);
  }

  @Test
  public void deleteImageEventShouldThrowWhenEventDoesNotExist() {
    //Arrange
    when(repository.findById(40L)).thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(ImageEventIsNotFoundException.class,
      () -> service.deleteImageEvent(40L));

    //Assert
    verify(repository, never()).delete(any(ImageEvent.class));
  }

  private void changeStatus(String action) {
    switch (action) {
      case "processing" -> service.markAsProcessing(40L);
      case "processed" -> service.markAsProcessed(40L);
      case "failed" -> service.markAsFailed(40L);
      default -> throw new IllegalArgumentException(action);
    }
  }

  private ImageEventResponseDto expectedDto() {
    return new ImageEventResponseDto(
      40L, 20L, 30L, "/images/test.jpg",
      time, time.plusSeconds(1), ImageProcessingStatus.RECEIVED
    );
  }
}
