package com.skr1l.backend.imageEvent;

import com.skr1l.backend.device.Device;
import com.skr1l.backend.device.enums.DeviceStatus;
import com.skr1l.backend.imageEvent.enums.ImageProcessingStatus;
import com.skr1l.backend.imageEvent.repository.ImageEventRepository;
import com.skr1l.backend.rack.Rack;
import com.skr1l.backend.shelf.Shelf;
import com.skr1l.backend.shelf.enums.ShelfStatus;
import com.skr1l.backend.warehouse.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
public class ImageEventRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private ImageEventRepository repository;

  private Rack rack;
  private Device device;
  private Device otherDevice;

  private final LocalDateTime time =
    LocalDateTime.of(2026, 10, 15, 12, 0);

  @BeforeEach
  public void setup() {
    Warehouse warehouse = new Warehouse();
    warehouse.setName("Test warehouse");
    warehouse.setAddress("Test address");
    entityManager.persist(warehouse);

    rack = new Rack();
    rack.setLabel("Rack A");
    rack.setWarehouse(warehouse);
    entityManager.persist(rack);

    device = persistDevice("CAM-001", "Shelf A");
    otherDevice = persistDevice("CAM-002", "Shelf B");
  }

  @Test
  public void findByDeviceIdShouldReturnOnlyMatchingEvents() {
    //Arrange
    ImageEvent matching =
      persistEvent(device, time, ImageProcessingStatus.RECEIVED);

    persistEvent(otherDevice, time, ImageProcessingStatus.RECEIVED);
    flushAndClear();

    //Act
    List<ImageEvent> result = repository.findByDeviceId(device.getId());

    //Assert
    assertEquals(Set.of(matching.getId()), ids(result));
  }

  @Test
  public void findByDeviceIdShouldReturnEmptyListWhenNoEventsMatch() {
    //Arrange
    persistEvent(otherDevice, time, ImageProcessingStatus.RECEIVED);
    flushAndClear();

    //Act
    List<ImageEvent> result = repository.findByDeviceId(device.getId());

    //Assert
    assertTrue(result.isEmpty());
  }

  @Test
  public void findByShelfIdShouldReturnOnlyMatchingEvents() {
    //Arrange
    ImageEvent matching =
      persistEvent(device, time, ImageProcessingStatus.RECEIVED);

    persistEvent(otherDevice, time, ImageProcessingStatus.RECEIVED);
    flushAndClear();

    //Act
    List<ImageEvent> result =
      repository.findByShelfId(device.getShelf().getId());

    //Assert
    assertEquals(Set.of(matching.getId()), ids(result));
  }

  @Test
  public void findByShelfIdShouldReturnEmptyListWhenNoEventsMatch() {
    //Arrange
    persistEvent(otherDevice, time, ImageProcessingStatus.RECEIVED);
    flushAndClear();

    //Act
    List<ImageEvent> result =
      repository.findByShelfId(device.getShelf().getId());

    //Assert
    assertTrue(result.isEmpty());
  }

  @Test
  public void findByImageProcessingStatusShouldReturnOnlyMatchingEvents() {
    //Arrange
    ImageEvent matching =
      persistEvent(device, time, ImageProcessingStatus.PROCESSED);

    persistEvent(device, time, ImageProcessingStatus.FAILED);
    flushAndClear();

    //Act
    List<ImageEvent> result =
      repository.findByImageProcessingStatus(ImageProcessingStatus.PROCESSED);

    //Assert
    assertEquals(Set.of(matching.getId()), ids(result));
  }

  @Test
  public void findByImageProcessingStatusShouldReturnEmptyList() {
    //Arrange
    persistEvent(device, time, ImageProcessingStatus.FAILED);
    flushAndClear();

    //Act
    List<ImageEvent> result =
      repository.findByImageProcessingStatus(ImageProcessingStatus.PROCESSED);

    //Assert
    assertTrue(result.isEmpty());
  }

  @Test
  public void findByCapturedAtBetweenShouldIncludeBothBoundaries() {
    //Arrange
    LocalDateTime to = time.plusHours(2);

    persistEvent(device, time.minusSeconds(1), ImageProcessingStatus.RECEIVED);
    ImageEvent first =
      persistEvent(device, time, ImageProcessingStatus.RECEIVED);
    ImageEvent middle =
      persistEvent(device, time.plusHours(1), ImageProcessingStatus.RECEIVED);
    ImageEvent last =
      persistEvent(device, to, ImageProcessingStatus.RECEIVED);
    persistEvent(device, to.plusSeconds(1), ImageProcessingStatus.RECEIVED);
    flushAndClear();

    //Act
    List<ImageEvent> result =
      repository.findByCapturedAtBetween(time, to);

    //Assert
    assertEquals(
      Set.of(first.getId(), middle.getId(), last.getId()),
      ids(result)
    );
  }

  @Test
  public void findByCapturedAtBetweenShouldSupportEqualBoundaries() {
    //Arrange
    ImageEvent matching =
      persistEvent(device, time, ImageProcessingStatus.RECEIVED);

    persistEvent(device, time.plusSeconds(1), ImageProcessingStatus.RECEIVED);
    flushAndClear();

    //Act
    List<ImageEvent> result =
      repository.findByCapturedAtBetween(time, time);

    //Assert
    assertEquals(Set.of(matching.getId()), ids(result));
  }

  @Test
  public void findByCapturedAtBetweenShouldReturnEmptyList() {
    //Arrange
    persistEvent(device, time.minusSeconds(1), ImageProcessingStatus.RECEIVED);
    flushAndClear();

    //Act
    List<ImageEvent> result =
      repository.findByCapturedAtBetween(time, time.plusHours(1));

    //Assert
    assertTrue(result.isEmpty());
  }

  @ParameterizedTest
  @CsvSource({
    "0, PROCESSED, true",
    "1, PROCESSED, true",
    "-1, PROCESSED, false",
    "0, FAILED, false"
  })
  public void existsShouldCheckStatusAndInclusiveCaptureTime(
    int offsetSeconds,
    ImageProcessingStatus status,
    boolean expected
  ) {
    //Arrange
    persistEvent(device, time.plusSeconds(offsetSeconds), status);
    flushAndClear();

    //Act
    boolean result = repository
      .existsByShelfIdAndImageProcessingStatusAndCapturedAtGreaterThanEqual(
        device.getShelf().getId(),
        ImageProcessingStatus.PROCESSED,
        time
      );

    //Assert
    assertEquals(expected, result);
  }

  @Test
  public void existsShouldReturnFalseForDifferentShelf() {
    //Arrange
    persistEvent(otherDevice, time, ImageProcessingStatus.PROCESSED);
    flushAndClear();

    //Act
    boolean result = repository
      .existsByShelfIdAndImageProcessingStatusAndCapturedAtGreaterThanEqual(
        device.getShelf().getId(),
        ImageProcessingStatus.PROCESSED,
        time
      );

    //Assert
    assertFalse(result);
  }

  private Device persistDevice(String key, String label) {
    Shelf shelf = new Shelf();
    shelf.setRack(rack);
    shelf.setLabel(label);
    shelf.setCapacity(100);
    shelf.setStatus(ShelfStatus.ACTIVE);
    entityManager.persist(shelf);

    Device result = new Device();
    result.setDeviceKey(key);
    result.setStatus(DeviceStatus.ACTIVE);
    result.setShelf(shelf);
    entityManager.persist(result);
    return result;
  }

  private ImageEvent persistEvent(
    Device device,
    LocalDateTime capturedAt,
    ImageProcessingStatus status
  ) {
    ImageEvent result = new ImageEvent();
    result.setDevice(device);
    result.setShelf(device.getShelf());
    result.setImagePath("/images/test.jpg");
    result.setCapturedAt(capturedAt);
    result.setReceivedAt(capturedAt.plusSeconds(1));
    result.setImageProcessingStatus(status);
    entityManager.persist(result);
    return result;
  }

  private Set<Long> ids(List<ImageEvent> events) {
    return events.stream()
      .map(ImageEvent::getId)
      .collect(Collectors.toSet());
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
