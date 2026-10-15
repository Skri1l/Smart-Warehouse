package com.skr1l.backend.demandPlan;

import com.skr1l.backend.demandPlan.repository.DemandPlanRepository;
import com.skr1l.backend.product.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class DemandPlanRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private DemandPlanRepository repository;

  private Product product;
  private final LocalDate date = LocalDate.of(2026, 10, 20);

  @BeforeEach
  public void setup() {
    product = persistProduct("COLA-001");
  }

  @Test
  public void findByProductIdShouldReturnOnlyMatchingPlans() {
    //Arrange
    Product otherProduct = persistProduct("OTHER-001");

    DemandPlan matching = persistPlan(product, date);
    persistPlan(otherProduct, date);
    flushAndClear();

    //Act
    List<DemandPlan> result = repository.findByProductId(product.getId());

    //Assert
    assertEquals(Set.of(matching.getId()), ids(result));
  }

  @Test
  public void findByProductIdShouldReturnEmptyListWhenNoPlansMatch() {
    //Arrange
    Product otherProduct = persistProduct("OTHER-001");
    persistPlan(otherProduct, date);
    flushAndClear();

    //Act
    List<DemandPlan> result = repository.findByProductId(product.getId());

    //Assert
    assertTrue(result.isEmpty());
  }

  @Test
  public void findByRequiredDateShouldReturnOnlyMatchingPlans() {
    //Arrange
    DemandPlan matching = persistPlan(product, date);
    persistPlan(product, date.minusDays(1));
    persistPlan(product, date.plusDays(1));
    flushAndClear();

    //Act
    List<DemandPlan> result = repository.findByRequiredDate(date);

    //Assert
    assertEquals(Set.of(matching.getId()), ids(result));
  }

  @Test
  public void findByRequiredDateShouldReturnEmptyListWhenNoPlansMatch() {
    //Arrange
    persistPlan(product, date.minusDays(1));
    flushAndClear();

    //Act
    List<DemandPlan> result = repository.findByRequiredDate(date);

    //Assert
    assertTrue(result.isEmpty());
  }

  @Test
  public void findByRequiredDateBetweenShouldIncludeBothBoundaries() {
    //Arrange
    LocalDate to = date.plusDays(2);

    persistPlan(product, date.minusDays(1));
    DemandPlan first = persistPlan(product, date);
    DemandPlan middle = persistPlan(product, date.plusDays(1));
    DemandPlan last = persistPlan(product, to);
    persistPlan(product, to.plusDays(1));
    flushAndClear();

    //Act
    List<DemandPlan> result =
      repository.findByRequiredDateBetween(date, to);

    //Assert
    assertEquals(
      Set.of(first.getId(), middle.getId(), last.getId()),
      ids(result)
    );
  }

  @Test
  public void findByRequiredDateBetweenShouldSupportEqualBoundaries() {
    //Arrange
    DemandPlan matching = persistPlan(product, date);
    persistPlan(product, date.plusDays(1));
    flushAndClear();

    //Act
    List<DemandPlan> result =
      repository.findByRequiredDateBetween(date, date);

    //Assert
    assertEquals(Set.of(matching.getId()), ids(result));
  }

  @Test
  public void findByRequiredDateBetweenShouldReturnEmptyListWhenNoPlansMatch() {
    //Arrange
    persistPlan(product, date.minusDays(1));
    flushAndClear();

    //Act
    List<DemandPlan> result =
      repository.findByRequiredDateBetween(date, date.plusDays(2));

    //Assert
    assertTrue(result.isEmpty());
  }

  @Test
  public void findByRequiredDateAfterShouldExcludeBoundaryAndEarlierDates() {
    //Arrange
    persistPlan(product, date.minusDays(1));
    persistPlan(product, date);
    DemandPlan matching = persistPlan(product, date.plusDays(1));
    flushAndClear();

    //Act
    List<DemandPlan> result = repository.findByRequiredDateAfter(date);

    //Assert
    assertEquals(Set.of(matching.getId()), ids(result));
  }

  @Test
  public void findByRequiredDateAfterShouldReturnEmptyListWhenNoPlansMatch() {
    //Arrange
    persistPlan(product, date);
    flushAndClear();

    //Act
    List<DemandPlan> result = repository.findByRequiredDateAfter(date);

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

  private DemandPlan persistPlan(Product product, LocalDate requiredDate) {
    DemandPlan result = new DemandPlan();
    result.setProduct(product);
    result.setRequiredDate(requiredDate);
    result.setRequiredQuantity(100);
    entityManager.persist(result);
    return result;
  }

  private Set<Long> ids(List<DemandPlan> plans) {
    return plans.stream()
      .map(DemandPlan::getId)
      .collect(Collectors.toSet());
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
