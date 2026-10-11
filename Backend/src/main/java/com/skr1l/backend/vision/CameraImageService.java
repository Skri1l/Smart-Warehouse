package com.skr1l.backend.vision;

import com.skr1l.backend.device.repository.DeviceRepository;
import com.skr1l.backend.exception.deviceExceptions.DeviceIsNotFoundException;
import com.skr1l.backend.vision.dto.CameraImageResponseDto;
import com.skr1l.backend.vision.dto.VisionResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class CameraImageService {

  private final VisionClient visionClient;
  private final CameraStockService cameraStockService;
  private final DeviceRepository deviceRepository;
  private final Path imageDirectory;
  private final long maxFileSize;

  public CameraImageService(VisionClient visionClient, CameraStockService cameraStockService,
                            DeviceRepository deviceRepository,
                            @Value("${vision.image-directory}") String imageDirectory,
                            @Value("${spring.servlet.multipart.max-file-size}") DataSize maxFileSize) {

    this.visionClient = visionClient;
    this.cameraStockService = cameraStockService;
    this.deviceRepository = deviceRepository;
    this.imageDirectory = Path.of(imageDirectory)
      .toAbsolutePath()
      .normalize();
    this.maxFileSize = maxFileSize.toBytes();
  }

  public CameraImageResponseDto process(
    Long deviceId,
    LocalDateTime capturedAt,
    MultipartFile file) {

    LocalDateTime receivedAt = LocalDateTime.now();

    if (!deviceRepository.existsById(deviceId)) {
      throw new DeviceIsNotFoundException("Device not found: " + deviceId);
    }

    if (file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image is empty");
    }

    Path imagePath = imageDirectory.resolve(UUID.randomUUID() + ".image");

    try {
      Files.createDirectories(imageDirectory);

      try (InputStream input = file.getInputStream()) {
        Files.copy(input, imagePath);
      }

      VisionResponseDto response = visionClient.recognize(file);

      return cameraStockService.apply(
        deviceId,
        capturedAt,
        receivedAt,
        imagePath.toString(),
        response.count());
    } catch (IOException exception) {
      deleteImage(imagePath);

      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
        "Cannot save camera image", exception);
    } catch (RuntimeException exception) {
      deleteImage(imagePath);
      throw exception;
    }
  }

  private void deleteImage(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException exception) {
      log.warn("Cannot delete camera image {}", path, exception);
    }
  }
}
