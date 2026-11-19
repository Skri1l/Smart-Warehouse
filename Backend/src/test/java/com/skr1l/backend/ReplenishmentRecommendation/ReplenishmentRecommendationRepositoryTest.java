package com.skr1l.backend.ReplenishmentRecommendation;

import com.skr1l.backend.product.Product;
import com.skr1l.backend.purchaseOrder.PurchaseOrder;
import com.skr1l.backend.purchaseOrder.enums.PurchaseOrderStatus;
import com.skr1l.backend.replenishmentRecommendation.ReplenishmentRecommendation;
import com.skr1l.backend.replenishmentRecommendation.enums.RecommendationStatus;
import com.skr1l.backend.replenishmentRecommendation.repository.ReplenishmentRecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ReplenishmentRecommendationRepositoryTest {

  private Product product;

  @Autowired
  private TestEntityManager entityManager;
  @Autowired
  private ReplenishmentRecommendationRepository replenishmentRecommendationRepository;

  @BeforeEach
  void setup() {
    product = new Product();
    product.setName("product");
    product.setSku("productSku");

    entityManager.persist(product);
  }

  @Test
  void findByProductIdShouldReturnListOfReplenishmentRecommendation() {
    //Arrange
    ReplenishmentRecommendation replenishmentRecommendation = createRecommendation();

    replenishmentRecommendationRepository.save(replenishmentRecommendation);

    //Act
    List<ReplenishmentRecommendation> result =  replenishmentRecommendationRepository
      .findByProductId(product.getId());

    //Assert
    assertEquals(1, result.size());
    assertEquals(replenishmentRecommendation.getId(), result.get(0).getId());
    assertEquals(product.getId(), result.get(0).getProduct().getId());
  }

  @Test
  void findByProductIdShouldReturnEmptyListWhenNoRecommendationsMatch(){
    //Arrange
    replenishmentRecommendationRepository.save(createRecommendation());

    Long absentProductId = product.getId() + 1;

    //Act
    List<ReplenishmentRecommendation> result =  replenishmentRecommendationRepository
      .findByProductId(absentProductId);

    //Assert
    assertEquals(0, result.size());
    assertEquals(List.of(), result);
  }

  @Test
  void findByStatusShouldReturnListOfReplenishmentRecommendation() {
    //Arrange
    ReplenishmentRecommendation replenishmentRecommendation = createRecommendation();

    replenishmentRecommendationRepository.save(replenishmentRecommendation);

    //Act
    List<ReplenishmentRecommendation> result  =
      replenishmentRecommendationRepository.findByStatus(replenishmentRecommendation.getStatus());

    //Assert
    assertEquals(1, result.size());
    assertEquals(replenishmentRecommendation.getId(), result.get(0).getId());
    assertEquals(replenishmentRecommendation.getStatus(), result.get(0).getStatus());
  }

  @Test
  void findByProductIdShouldExcludeRecommendationsForOtherProducts() {
    //Arrange
    Product otherProduct = new Product();
    otherProduct.setName("other product");
    otherProduct.setSku("otherProductSku");
    entityManager.persist(otherProduct);

    ReplenishmentRecommendation matching = createRecommendation();
    ReplenishmentRecommendation other = createRecommendation();
    other.setProduct(otherProduct);

    entityManager.persist(matching);
    entityManager.persist(other);
    entityManager.flush();
    entityManager.clear();

    //Act
    List<ReplenishmentRecommendation> result =
      replenishmentRecommendationRepository.findByProductId(product.getId());

    //Assert
    assertEquals(1, result.size());
    assertEquals(matching.getId(), result.get(0).getId());
    assertEquals(product.getId(), result.get(0).getProduct().getId());
  }

  @Test
  void findByStatusShouldExcludeRecommendationsWithOtherStatuses() {
    //Arrange
    ReplenishmentRecommendation matching = createRecommendation();
    ReplenishmentRecommendation other = createRecommendation();
    other.setStatus(RecommendationStatus.APPROVED);

    entityManager.persist(matching);
    entityManager.persist(other);
    entityManager.flush();
    entityManager.clear();

    //Act
    List<ReplenishmentRecommendation> result =
      replenishmentRecommendationRepository.findByStatus(RecommendationStatus.NEW);

    //Assert
    assertEquals(1, result.size());
    assertEquals(matching.getId(), result.get(0).getId());
    assertEquals(RecommendationStatus.NEW, result.get(0).getStatus());
  }

  @Test
  void findByStatusShouldReturnEmptyListWhenNoRecommendationsMatch() {
    //Arrange
    entityManager.persist(createRecommendation());
    entityManager.flush();
    entityManager.clear();

    //Act
    List<ReplenishmentRecommendation> result =
      replenishmentRecommendationRepository
        .findByStatus(RecommendationStatus.APPROVED);

    //Assert
    assertTrue(result.isEmpty());
  }

  @Test
  void findByRequiredByShouldReturnOnlyRecommendationsForExactDate() {
    //Arrange
    LocalDate requiredBy = LocalDate.of(2026, 10, 20);

    ReplenishmentRecommendation matching = createRecommendation();
    matching.setRequiredBy(requiredBy);

    ReplenishmentRecommendation earlier = createRecommendation();
    earlier.setRequiredBy(requiredBy.minusDays(1));

    ReplenishmentRecommendation later = createRecommendation();
    later.setRequiredBy(requiredBy.plusDays(1));

    entityManager.persist(matching);
    entityManager.persist(earlier);
    entityManager.persist(later);
    entityManager.flush();
    entityManager.clear();

    //Act
    List<ReplenishmentRecommendation> result =
      replenishmentRecommendationRepository.findByRequiredBy(requiredBy);

    //Assert
    assertEquals(1, result.size());
    assertEquals(matching.getId(), result.get(0).getId());
    assertEquals(requiredBy, result.get(0).getRequiredBy());
  }

  @Test
  void findByRequiredByShouldReturnEmptyListWhenNoRecommendationsMatch() {
    //Arrange
    LocalDate requiredBy = LocalDate.of(2026, 10, 20);

    ReplenishmentRecommendation recommendation = createRecommendation();
    recommendation.setRequiredBy(requiredBy.minusDays(1));

    entityManager.persist(recommendation);
    entityManager.flush();
    entityManager.clear();

    //Act
    List<ReplenishmentRecommendation> result =
      replenishmentRecommendationRepository.findByRequiredBy(requiredBy);

    //Assert
    assertTrue(result.isEmpty());
  }

  @Test
  void findByRequiredByBetweenShouldIncludeBoundariesAndExcludeOutsideDates() {
    //Arrange
    LocalDate from = LocalDate.of(2026, 10, 10);
    LocalDate to = LocalDate.of(2026, 10, 20);

    ReplenishmentRecommendation beforeRange = createRecommendation();
    beforeRange.setRequiredBy(from.minusDays(1));

    ReplenishmentRecommendation atStart = createRecommendation();
    atStart.setRequiredBy(from);

    ReplenishmentRecommendation insideRange = createRecommendation();
    insideRange.setRequiredBy(from.plusDays(5));

    ReplenishmentRecommendation atEnd = createRecommendation();
    atEnd.setRequiredBy(to);

    ReplenishmentRecommendation afterRange = createRecommendation();
    afterRange.setRequiredBy(to.plusDays(1));

    entityManager.persist(beforeRange);
    entityManager.persist(atStart);
    entityManager.persist(insideRange);
    entityManager.persist(atEnd);
    entityManager.persist(afterRange);
    entityManager.flush();
    entityManager.clear();

    //Act
    List<ReplenishmentRecommendation> result =
      replenishmentRecommendationRepository.findByRequiredByBetween(from, to);

    //Assert
    assertEquals(3, result.size());

    List<Long> actualIds = result.stream()
      .map(ReplenishmentRecommendation::getId)
      .sorted()
      .toList();

    List<Long> expectedIds = List.of(
      atStart.getId(),
      insideRange.getId(),
      atEnd.getId()
    ).stream().sorted().toList();

    assertEquals(expectedIds, actualIds);
  }

  @Test
  void findByRequiredByBetweenShouldReturnEmptyListWhenNoRecommendationsMatch() {
    //Arrange
    LocalDate from = LocalDate.of(2026, 10, 10);
    LocalDate to = LocalDate.of(2026, 10, 20);

    ReplenishmentRecommendation earlier = createRecommendation();
    earlier.setRequiredBy(from.minusDays(1));

    ReplenishmentRecommendation later = createRecommendation();
    later.setRequiredBy(to.plusDays(1));

    entityManager.persist(earlier);
    entityManager.persist(later);
    entityManager.flush();
    entityManager.clear();

    //Act
    List<ReplenishmentRecommendation> result =
      replenishmentRecommendationRepository.findByRequiredByBetween(from, to);

    //Assert
    assertTrue(result.isEmpty());
  }

  @Test
  void findByRequiredByBetweenShouldMatchExactDateWhenBoundariesAreEqual() {
    //Arrange
    LocalDate date = LocalDate.of(2026, 10, 20);

    ReplenishmentRecommendation matching = createRecommendation();
    matching.setRequiredBy(date);

    ReplenishmentRecommendation other = createRecommendation();
    other.setRequiredBy(date.plusDays(1));

    entityManager.persist(matching);
    entityManager.persist(other);
    entityManager.flush();
    entityManager.clear();

    //Act
    List<ReplenishmentRecommendation> result =
      replenishmentRecommendationRepository.findByRequiredByBetween(date, date);

    //Assert
    assertEquals(1, result.size());
    assertEquals(matching.getId(), result.get(0).getId());
    assertEquals(date, result.get(0).getRequiredBy());
  }

  private ReplenishmentRecommendation createRecommendation() {
    ReplenishmentRecommendation recommendation = new ReplenishmentRecommendation();

    recommendation.setProduct(product);
    recommendation.setRecommendedQuantity(100);
    recommendation.setRequiredBy(LocalDate.now().minusDays(1));
    recommendation.setReason("reason");
    recommendation.setStatus(RecommendationStatus.NEW);
    recommendation.setCreatedAt(LocalDateTime.now());

    return recommendation;
  }
}
