package com.skr1l.backend.vision;

import com.skr1l.backend.device.Device;
import com.skr1l.backend.device.enums.DeviceStatus;
import com.skr1l.backend.device.repository.DeviceRepository;
import com.skr1l.backend.exception.deviceExceptions.DeviceIsNotFoundException;
import com.skr1l.backend.imageEvent.ImageEvent;
import com.skr1l.backend.imageEvent.enums.ImageProcessingStatus;
import com.skr1l.backend.imageEvent.repository.ImageEventRepository;
import com.skr1l.backend.recognitionResult.RecognitionResult;
import com.skr1l.backend.recognitionResult.enums.RecognitionStatus;
import com.skr1l.backend.recognitionResult.repository.RecognitionResultRepository;
import com.skr1l.backend.shelfStock.ShelfStock;
import com.skr1l.backend.shelfStock.repository.ShelfStockRepository;
import com.skr1l.backend.stockMovement.StockMovement;
import com.skr1l.backend.stockMovement.enums.StockMovementSource;
import com.skr1l.backend.stockMovement.enums.StockMovementType;
import com.skr1l.backend.stockMovement.repository.StockMovementRepository;
import com.skr1l.backend.vision.dto.CameraImageResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CameraStockService {

  private final DeviceRepository deviceRepository;
  private final ShelfStockRepository shelfStockRepository;
  private final ImageEventRepository imageEventRepository;
  private final RecognitionResultRepository recognitionResultRepository;
  private final StockMovementRepository stockMovementRepository;

  @Transactional
  public CameraImageResponseDto apply(Long deviceId, LocalDateTime capturedAt,
                                      LocalDateTime receivedAt, String imagePath, int count) {

    if (count < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Vision service returned a negative count");
    }

    Device device = deviceRepository.findById(deviceId)
      .orElseThrow(() -> new DeviceIsNotFoundException("Device not found: " + deviceId));

    if (device.getStatus() != DeviceStatus.ACTIVE) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Device is inactive");
    }

    Long shelfId = device.getShelf().getId();

    List<ShelfStock> stocks = shelfStockRepository.findAllByShelfId(shelfId);

    if (stocks.size() != 1) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
        "Camera processing requires exactly one stock record on the shelf");
    }

    if (imageEventRepository
      .existsByShelfIdAndImageProcessingStatusAndCapturedAtGreaterThanEqual(shelfId,
        ImageProcessingStatus.PROCESSED, capturedAt)) {

      throw new ResponseStatusException(HttpStatus.CONFLICT,
        "An image with the same or a later capture time was already processed");
    }

    ShelfStock stock = stocks.getFirst();

    int previousQuantity = stock.getQuantity();
    int delta = Math.subtractExact(count, previousQuantity);

    ImageEvent event = new ImageEvent();
    event.setDevice(device);
    event.setShelf(device.getShelf());
    event.setImagePath(imagePath);
    event.setCapturedAt(capturedAt);
    event.setReceivedAt(receivedAt);
    event.setImageProcessingStatus(ImageProcessingStatus.PROCESSED);

    imageEventRepository.save(event);

    RecognitionResult result = new RecognitionResult();
    result.setImageEvent(event);
    result.setProduct(stock.getProduct());
    result.setDetectedQuantity(count);
    result.setQuantityDelta(delta);
    result.setConfidence(null);
    result.setStatus(RecognitionStatus.CONFIRMED);

    recognitionResultRepository.save(result);

    stock.setQuantity(count);

    if (delta != 0) {
      StockMovement movement = new StockMovement();
      movement.setProduct(stock.getProduct());
      movement.setShelf(stock.getShelf());
      movement.setQuantityDelta(delta);
      movement.setType(StockMovementType.ADJUSTMENT);
      movement.setSource(StockMovementSource.CAMERA);
      movement.setCreatedAt(LocalDateTime.now());
      movement.setImageEvent(event);

      stockMovementRepository.save(movement);
    }

    device.setLastSeenAt(receivedAt);

    return new CameraImageResponseDto(
      event.getId(),
      result.getId(),
      shelfId,
      previousQuantity,
      count,
      delta);
  }
}
