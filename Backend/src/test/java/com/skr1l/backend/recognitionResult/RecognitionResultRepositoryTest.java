package com.skr1l.backend.recognitionResult;

import com.skr1l.backend.device.Device;
import com.skr1l.backend.device.enums.DeviceStatus;
import com.skr1l.backend.imageEvent.ImageEvent;
import com.skr1l.backend.imageEvent.enums.ImageProcessingStatus;
import com.skr1l.backend.product.Product;
import com.skr1l.backend.rack.Rack;
import com.skr1l.backend.recognitionResult.enums.RecognitionStatus;
import com.skr1l.backend.recognitionResult.repository.RecognitionResultRepository;
import com.skr1l.backend.shelf.Shelf;
import com.skr1l.backend.shelf.enums.ShelfStatus;
import com.skr1l.backend.warehouse.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RecognitionResultRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private RecognitionResultRepository repository;

  private Product product;
  private Device device;

  @BeforeEach
  public void setup() {
    product = persistProduct("COLA-001");

    Warehouse warehouse = new Warehouse();
    warehouse.setName("Test warehouse");
    warehouse.setAddress("Test address");
    entityManager.persist(warehouse);

    Rack rack = new Rack();
    rack.setLabel("Rack A");
    rack.setWarehouse(warehouse);
    entityManager.persist(rack);

    Shelf shelf = new Shelf();
    shelf.setLabel("Shelf A");
    shelf.setCapacity(100);
    shelf.setStatus(ShelfStatus.ACTIVE);
    shelf.setRack(rack);
    entityManager.persist(shelf);

    device = new Device();
    device.setDeviceKey("CAM-001");
    device.setStatus(DeviceStatus.ACTIVE);
    device.setShelf(shelf);
    entityManager.persist(device);
  }

  @Test
  public void findByImageEventIdShouldReturnOnlyMatchingResults() {
    //Arrange
    ImageEvent image = persistImageEvent();

    RecognitionResult matching =
      persistResult(image, product, RecognitionStatus.DETECTED, 0.95);

    persistResult(
      persistImageEvent(), product, RecognitionStatus.DETECTED, 0.95
    );
    flushAndClear();

    //Act
    List<RecognitionResult> result =
      repository.findByImageEventId(image.getId());

    //Assert
    assertEquals(Set.of(matching.getId()), ids(result));
  }

  @Test
  public void findByImageEventIdShouldReturnEmptyList() {
    //Arrange
    ImageEvent imageWithoutResult = persistImageEvent();

    persistResult(
      persistImageEvent(), product, RecognitionStatus.DETECTED, 0.95
    );
    flushAndClear();

    //Act
    List<RecognitionResult> result =
      repository.findByImageEventId(imageWithoutResult.getId());

    //Assert
    assertTrue(result.isEmpty());
  }

  @Test
  public void findByProductIdShouldReturnOnlyMatchingResults() {
    //Arrange
    Product other = persistProduct("OTHER-001");

    RecognitionResult matching = persistResult(
      persistImageEvent(), product, RecognitionStatus.DETECTED, 0.95
    );

    persistResult(
      persistImageEvent(), other, RecognitionStatus.DETECTED, 0.95
    );
    flushAndClear();

    //Act
    List<RecognitionResult> result =
      repository.findByProductId(product.getId());

    //Assert
    assertEquals(Set.of(matching.getId()), ids(result));
  }

  @Test
  public void findByProductIdShouldReturnEmptyList() {
    //Arrange
    Product other = persistProduct("OTHER-001");

    persistResult(
      persistImageEvent(), other, RecognitionStatus.DETECTED, 0.95
    );
    flushAndClear();

    //Act
    List<RecognitionResult> result =
      repository.findByProductId(product.getId());

    //Assert
    assertTrue(result.isEmpty());
  }

  @Test
  public void findByStatusShouldReturnOnlyMatchingResults() {
    //Arrange
    RecognitionResult matching = persistResult(
      persistImageEvent(), product, RecognitionStatus.CONFIRMED, 0.95
    );

    persistResult(
      persistImageEvent(), product, RecognitionStatus.REJECTED, 0.95
    );
    flushAndClear();

    //Act
    List<RecognitionResult> result =
      repository.findByStatus(RecognitionStatus.CONFIRMED);

    //Assert
    assertEquals(Set.of(matching.getId()), ids(result));
  }

  @Test
  public void findByStatusShouldReturnEmptyList() {
    //Arrange
    persistResult(
      persistImageEvent(), product, RecognitionStatus.REJECTED, 0.95
    );
    flushAndClear();

    //Act
    List<RecognitionResult> result =
      repository.findByStatus(RecognitionStatus.CONFIRMED);

    //Assert
    assertTrue(result.isEmpty());
  }

  @Test
  public void findByConfidenceShouldIncludeBoundaryAndHigherValues() {
    //Arrange
    persistResult(
      persistImageEvent(), product, RecognitionStatus.DETECTED, 0.5
    );

    RecognitionResult equal = persistResult(
      persistImageEvent(), product, RecognitionStatus.DETECTED, 0.75
    );

    RecognitionResult higher = persistResult(
      persistImageEvent(), product, RecognitionStatus.DETECTED, 0.9
    );

    persistResult(
      persistImageEvent(), product, RecognitionStatus.DETECTED, null
    );
    flushAndClear();

    //Act
    List<RecognitionResult> result =
      repository.findByConfidenceGreaterThanEqual(0.75);

    //Assert
    assertEquals(Set.of(equal.getId(), higher.getId()), ids(result));
  }

  @Test
  public void findByConfidenceShouldReturnEmptyListWhenNoResultsMatch() {
    //Arrange
    persistResult(
      persistImageEvent(), product, RecognitionStatus.DETECTED, 0.5
    );
    flushAndClear();

    //Act
    List<RecognitionResult> result =
      repository.findByConfidenceGreaterThanEqual(0.75);

    //Assert
    assertTrue(result.isEmpty());
  }

  private Product persistProduct(String sku) {
    Product result = new Product();
    result.setName("Product");
    result.setSku(sku);
    entityManager.persist(result);
    return result;
  }

  private ImageEvent persistImageEvent() {
    LocalDateTime time = LocalDateTime.of(2026, 10, 15, 12, 0);

    ImageEvent result = new ImageEvent();
    result.setDevice(device);
    result.setShelf(device.getShelf());
    result.setImagePath("/images/test.jpg");
    result.setCapturedAt(time);
    result.setReceivedAt(time.plusSeconds(1));
    result.setImageProcessingStatus(ImageProcessingStatus.PROCESSED);
    entityManager.persist(result);
    return result;
  }

  private RecognitionResult persistResult(
    ImageEvent image,
    Product product,
    RecognitionStatus status,
    Double confidence
  ) {
    RecognitionResult result = new RecognitionResult();
    result.setImageEvent(image);
    result.setProduct(product);
    result.setDetectedQuantity(14);
    result.setQuantityDelta(-2);
    result.setConfidence(confidence);
    result.setStatus(status);
    entityManager.persist(result);
    return result;
  }

  private Set<Long> ids(List<RecognitionResult> results) {
    return results.stream()
      .map(RecognitionResult::getId)
      .collect(Collectors.toSet());
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
