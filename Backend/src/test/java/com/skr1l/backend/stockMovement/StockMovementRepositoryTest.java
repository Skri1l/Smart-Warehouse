package com.skr1l.backend.stockMovement;

import com.skr1l.backend.product.Product;
import com.skr1l.backend.rack.Rack;
import com.skr1l.backend.shelf.Shelf;
import com.skr1l.backend.shelf.enums.ShelfStatus;
import com.skr1l.backend.stockMovement.enums.*;
import com.skr1l.backend.stockMovement.repository.StockMovementRepository;
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
public class StockMovementRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private StockMovementRepository repository;

  private Product product;
  private Shelf shelf;
  private Rack rack;

  private final LocalDateTime time =
    LocalDateTime.of(2026, 10, 15, 12, 0);

  @BeforeEach
  public void setup() {
    product = persistProduct("COLA-001");

    Warehouse warehouse = new Warehouse();
    warehouse.setName("Test warehouse");
    warehouse.setAddress("Test address");
    entityManager.persist(warehouse);

    rack = new Rack();
    rack.setLabel("Rack A");
    rack.setWarehouse(warehouse);
    entityManager.persist(rack);

    shelf = persistShelf("Shelf A");
  }

  @Test
  public void findByProductIdShouldReturnOnlyMatchingMovements() {
    //Arrange
    StockMovement matching = newMovement();
    entityManager.persist(matching);

    StockMovement other = newMovement();
    other.setProduct(persistProduct("OTHER-001"));
    entityManager.persist(other);
    flushAndClear();

    //Act
    List<StockMovement> result =
      repository.findByProductId(product.getId());

    //Assert
    assertEquals(Set.of(matching.getId()), ids(result));
  }

  @Test
  public void findByProductIdShouldReturnEmptyList() {
    //Arrange
    StockMovement other = newMovement();
    other.setProduct(persistProduct("OTHER-001"));
    entityManager.persist(other);
    flushAndClear();

    //Act
    List<StockMovement> result =
      repository.findByProductId(product.getId());

    //Assert
    assertTrue(result.isEmpty());
  }

  @Test
  public void findByShelfIdShouldReturnOnlyMatchingMovements() {
    //Arrange
    StockMovement matching = newMovement();
    entityManager.persist(matching);

    StockMovement other = newMovement();
    other.setShelf(persistShelf("Shelf B"));
    entityManager.persist(other);
    flushAndClear();

    //Act
    List<StockMovement> result =
      repository.findByShelfId(shelf.getId());

    //Assert
    assertEquals(Set.of(matching.getId()), ids(result));
  }

  @Test
  public void findByShelfIdShouldReturnEmptyList() {
    //Arrange
    StockMovement other = newMovement();
    other.setShelf(persistShelf("Shelf B"));
    entityManager.persist(other);
    flushAndClear();

    //Act
    List<StockMovement> result =
      repository.findByShelfId(shelf.getId());

    //Assert
    assertTrue(result.isEmpty());
  }

  @Test
  public void findByTypeShouldReturnOnlyMatchingMovements() {
    //Arrange
    StockMovement matching = newMovement();
    entityManager.persist(matching);

    StockMovement other = newMovement();
    other.setType(StockMovementType.RECEIPT);
    entityManager.persist(other);
    flushAndClear();

    //Act
    List<StockMovement> result =
      repository.findByType(StockMovementType.REMOVAL);

    //Assert
    assertEquals(Set.of(matching.getId()), ids(result));
  }

  @Test
  public void findByTypeShouldReturnEmptyList() {
    //Arrange
    entityManager.persist(newMovement());
    flushAndClear();

    //Act
    List<StockMovement> result =
      repository.findByType(StockMovementType.RECEIPT);

    //Assert
    assertTrue(result.isEmpty());
  }

  @Test
  public void findBySourceShouldReturnOnlyMatchingMovements() {
    //Arrange
    StockMovement matching = newMovement();
    entityManager.persist(matching);

    StockMovement other = newMovement();
    other.setSource(StockMovementSource.MANUAL);
    entityManager.persist(other);
    flushAndClear();

    //Act
    List<StockMovement> result =
      repository.findBySource(StockMovementSource.CAMERA);

    //Assert
    assertEquals(Set.of(matching.getId()), ids(result));
  }

  @Test
  public void findBySourceShouldReturnEmptyList() {
    //Arrange
    entityManager.persist(newMovement());
    flushAndClear();

    //Act
    List<StockMovement> result =
      repository.findBySource(StockMovementSource.MANUAL);

    //Assert
    assertTrue(result.isEmpty());
  }

  @Test
  public void findByCreatedAtBetweenShouldIncludeBothBoundaries() {
    //Arrange
    LocalDateTime to = time.plusHours(2);

    StockMovement before = newMovement();
    before.setCreatedAt(time.minusSeconds(1));

    StockMovement first = newMovement();

    StockMovement middle = newMovement();
    middle.setCreatedAt(time.plusHours(1));

    StockMovement last = newMovement();
    last.setCreatedAt(to);

    StockMovement after = newMovement();
    after.setCreatedAt(to.plusSeconds(1));

    for (StockMovement movement :
      List.of(before, first, middle, last, after)) {
      entityManager.persist(movement);
    }

    flushAndClear();

    //Act
    List<StockMovement> result =
      repository.findByCreatedAtBetween(time, to);

    //Assert
    assertEquals(
      Set.of(first.getId(), middle.getId(), last.getId()),
      ids(result)
    );
  }

  @Test
  public void findByCreatedAtBetweenShouldSupportEqualBoundaries() {
    //Arrange
    StockMovement matching = newMovement();
    entityManager.persist(matching);

    StockMovement other = newMovement();
    other.setCreatedAt(time.plusSeconds(1));
    entityManager.persist(other);
    flushAndClear();

    //Act
    List<StockMovement> result =
      repository.findByCreatedAtBetween(time, time);

    //Assert
    assertEquals(Set.of(matching.getId()), ids(result));
  }

  @Test
  public void findByCreatedAtBetweenShouldReturnEmptyList() {
    //Arrange
    StockMovement other = newMovement();
    other.setCreatedAt(time.minusSeconds(1));
    entityManager.persist(other);
    flushAndClear();

    //Act
    List<StockMovement> result =
      repository.findByCreatedAtBetween(time, time.plusHours(1));

    //Assert
    assertTrue(result.isEmpty());
  }

  private StockMovement newMovement() {
    StockMovement result = new StockMovement();
    result.setProduct(product);
    result.setShelf(shelf);
    result.setQuantityDelta(-2);
    result.setType(StockMovementType.REMOVAL);
    result.setSource(StockMovementSource.CAMERA);
    result.setCreatedAt(time);
    return result;
  }

  private Product persistProduct(String sku) {
    Product result = new Product();
    result.setName("Product");
    result.setSku(sku);
    entityManager.persist(result);
    return result;
  }

  private Shelf persistShelf(String label) {
    Shelf result = new Shelf();
    result.setLabel(label);
    result.setCapacity(100);
    result.setStatus(ShelfStatus.ACTIVE);
    result.setRack(rack);
    entityManager.persist(result);
    return result;
  }

  private Set<Long> ids(List<StockMovement> movements) {
    return movements.stream()
      .map(StockMovement::getId)
      .collect(Collectors.toSet());
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
