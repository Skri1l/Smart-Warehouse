package com.skr1l.backend.vision;

import com.skr1l.backend.device.repository.DeviceRepository;
import com.skr1l.backend.exception.deviceExceptions.DeviceIsNotFoundException;
import com.skr1l.backend.vision.dto.CameraImageResponseDto;
import com.skr1l.backend.vision.dto.VisionResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CameraImageServiceTest {

  private static final Long DEVICE_ID = 1L;
  private static final int DETECTED_COUNT = 15;

  private static final LocalDateTime CAPTURED_AT =
    LocalDateTime.of(2026, 10, 15, 12, 0);

  @Mock
  private VisionClient visionClient;

  @Mock
  private CameraStockService cameraStockService;

  @Mock
  private DeviceRepository deviceRepository;

  @TempDir
  Path tempDirectory;

  private CameraImageService service;
  private Path imageDirectory;
  private MockMultipartFile file;

  @BeforeEach
  void setUp() {
    imageDirectory = tempDirectory.resolve("images");

    service = new CameraImageService(
      visionClient,
      cameraStockService,
      deviceRepository,
      imageDirectory.toString(),
      DataSize.ofMegabytes(5)
    );

    file = new MockMultipartFile(
      "file",
      "camera.jpg",
      "image/jpeg",
      new byte[]{1, 2, 3}
    );
  }

  @Test
  void process_shouldSaveImageAndReturnStockServiceResponse()
    throws IOException {

    when(deviceRepository.existsById(DEVICE_ID)).thenReturn(true);

    VisionResponseDto visionResponse = mock(VisionResponseDto.class);
    when(visionResponse.count()).thenReturn(DETECTED_COUNT);
    when(visionClient.recognize(file)).thenReturn(visionResponse);

    CameraImageResponseDto expected =
      mock(CameraImageResponseDto.class);

    when(cameraStockService.apply(
      eq(DEVICE_ID),
      eq(CAPTURED_AT),
      any(LocalDateTime.class),
      anyString(),
      eq(DETECTED_COUNT)
    )).thenReturn(expected);

    LocalDateTime before = LocalDateTime.now();

    CameraImageResponseDto actual =
      service.process(DEVICE_ID, CAPTURED_AT, file);

    LocalDateTime after = LocalDateTime.now();

    ArgumentCaptor<String> pathCaptor =
      ArgumentCaptor.forClass(String.class);

    ArgumentCaptor<LocalDateTime> receivedAtCaptor =
      ArgumentCaptor.forClass(LocalDateTime.class);

    verify(cameraStockService).apply(
      eq(DEVICE_ID),
      eq(CAPTURED_AT),
      receivedAtCaptor.capture(),
      pathCaptor.capture(),
      eq(DETECTED_COUNT)
    );

    Path savedImage = Path.of(pathCaptor.getValue());

    assertSame(expected, actual);
    assertEquals(imageDirectory, savedImage.getParent());
    assertTrue(savedImage.getFileName().toString().endsWith(".image"));
    assertArrayEquals(file.getBytes(), Files.readAllBytes(savedImage));

    LocalDateTime receivedAt = receivedAtCaptor.getValue();
    assertFalse(receivedAt.isBefore(before));
    assertFalse(receivedAt.isAfter(after));

    verify(visionClient).recognize(file);
    assertImageCount(1);
  }

  @Test
  void process_shouldThrowWhenDeviceDoesNotExist() {
    when(deviceRepository.existsById(DEVICE_ID)).thenReturn(false);

    DeviceIsNotFoundException exception = assertThrows(
      DeviceIsNotFoundException.class,
      () -> service.process(DEVICE_ID, CAPTURED_AT, file)
    );

    assertEquals(
      "Device not found: " + DEVICE_ID,
      exception.getMessage()
    );

    verifyNoInteractions(visionClient, cameraStockService);
    assertFalse(Files.exists(imageDirectory));
  }

  @Test
  void process_shouldRejectEmptyImage() {
    when(deviceRepository.existsById(DEVICE_ID)).thenReturn(true);

    MockMultipartFile emptyFile = new MockMultipartFile(
      "file",
      "empty.jpg",
      "image/jpeg",
      new byte[0]
    );

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> service.process(DEVICE_ID, CAPTURED_AT, emptyFile)
    );

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    assertEquals("Image is empty", exception.getReason());

    verifyNoInteractions(visionClient, cameraStockService);
    assertFalse(Files.exists(imageDirectory));
  }

  @Test
  void process_shouldReturnInternalServerErrorWhenReadingFails()
    throws IOException {

    when(deviceRepository.existsById(DEVICE_ID)).thenReturn(true);

    MultipartFile brokenFile = mock(MultipartFile.class);
    IOException failure = new IOException("Cannot read image");

    when(brokenFile.isEmpty()).thenReturn(false);
    when(brokenFile.getInputStream()).thenThrow(failure);

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> service.process(DEVICE_ID, CAPTURED_AT, brokenFile)
    );

    assertEquals(
      HttpStatus.INTERNAL_SERVER_ERROR,
      exception.getStatusCode()
    );
    assertEquals("Cannot save camera image", exception.getReason());
    assertSame(failure, exception.getCause());

    verifyNoInteractions(visionClient, cameraStockService);
    assertImageCount(0);
  }

  @Test
  void process_shouldReturnInternalServerErrorWhenDirectoryCannotBeCreated()
    throws IOException {

    when(deviceRepository.existsById(DEVICE_ID)).thenReturn(true);

    Files.writeString(imageDirectory, "blocking file");

    ResponseStatusException exception = assertThrows(
      ResponseStatusException.class,
      () -> service.process(DEVICE_ID, CAPTURED_AT, file)
    );

    assertEquals(
      HttpStatus.INTERNAL_SERVER_ERROR,
      exception.getStatusCode()
    );
    assertEquals("Cannot save camera image", exception.getReason());
    assertInstanceOf(IOException.class, exception.getCause());

    assertEquals("blocking file", Files.readString(imageDirectory));
    verifyNoInteractions(visionClient, cameraStockService);
  }

  @Test
  void process_shouldDeleteImageWhenRecognitionFails() throws IOException {
    when(deviceRepository.existsById(DEVICE_ID)).thenReturn(true);

    RuntimeException failure =
      new RuntimeException("Vision service unavailable");

    when(visionClient.recognize(file)).thenThrow(failure);

    RuntimeException exception = assertThrows(
      RuntimeException.class,
      () -> service.process(DEVICE_ID, CAPTURED_AT, file)
    );

    assertSame(failure, exception);
    verify(visionClient).recognize(file);
    verifyNoInteractions(cameraStockService);
    assertImageCount(0);
  }

  @Test
  void process_shouldDeleteImageWhenStockUpdateFails() throws IOException {
    when(deviceRepository.existsById(DEVICE_ID)).thenReturn(true);

    VisionResponseDto visionResponse = mock(VisionResponseDto.class);
    when(visionResponse.count()).thenReturn(DETECTED_COUNT);
    when(visionClient.recognize(file)).thenReturn(visionResponse);

    RuntimeException failure =
      new RuntimeException("Stock update failed");

    when(cameraStockService.apply(
      eq(DEVICE_ID),
      eq(CAPTURED_AT),
      any(LocalDateTime.class),
      anyString(),
      eq(DETECTED_COUNT)
    )).thenThrow(failure);

    RuntimeException exception = assertThrows(
      RuntimeException.class,
      () -> service.process(DEVICE_ID, CAPTURED_AT, file)
    );

    assertSame(failure, exception);

    verify(visionClient).recognize(file);
    verify(cameraStockService).apply(
      eq(DEVICE_ID),
      eq(CAPTURED_AT),
      any(LocalDateTime.class),
      anyString(),
      eq(DETECTED_COUNT)
    );

    assertImageCount(0);
  }

  private void assertImageCount(long expected) throws IOException {
    try (var images = Files.list(imageDirectory)) {
      assertEquals(expected, images.count());
    }
  }
}
