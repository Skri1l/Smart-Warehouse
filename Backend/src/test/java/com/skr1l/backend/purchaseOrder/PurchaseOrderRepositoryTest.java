package com.skr1l.backend.purchaseOrder;

import com.skr1l.backend.product.Product;
import com.skr1l.backend.purchaseOrder.enums.PurchaseOrderStatus;
import com.skr1l.backend.purchaseOrder.repository.PurchaseOrderRepository;
import com.skr1l.backend.replenishmentRecommendation.ReplenishmentRecommendation;
import com.skr1l.backend.replenishmentRecommendation.enums.RecommendationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class PurchaseOrderRepositoryTest {

  private Product  product;

  private ReplenishmentRecommendation replenishmentRecommendation;

  @Autowired
  private PurchaseOrderRepository purchaseOrderRepository;

  @Autowired
  private TestEntityManager entityManager;

  @BeforeEach
  void setUp() {
    product = new Product();
    product.setName("product");
    product.setSku("productSku");

    entityManager.persist(product);

    replenishmentRecommendation = new ReplenishmentRecommendation();
    replenishmentRecommendation.setProduct(product);
    replenishmentRecommendation.setRecommendedQuantity(100);
    replenishmentRecommendation.setRequiredBy(LocalDate.now().plusWeeks(1));
    replenishmentRecommendation.setReason("reason");
    replenishmentRecommendation.setStatus(RecommendationStatus.NEW);
    replenishmentRecommendation.setCreatedAt(LocalDateTime.now());

    entityManager.persist(replenishmentRecommendation);
  }

  @Test
  void findByProductIdShouldReturnListOfPurchaseOrders() {
    //Arrange
    PurchaseOrder purchaseOrder = createOrder();

    purchaseOrderRepository.save(purchaseOrder);

    //Act
    List<PurchaseOrder> result = purchaseOrderRepository.findByProductId(product.getId());

    //Assert
    assertEquals(1, result.size());
    assertEquals(purchaseOrder.getId(), result.get(0).getId());
    assertEquals(product.getId(), result.get(0).getProduct().getId());
  }

  @Test
  void findByProductIdShouldReturnEmptyListWhenNoOrdersMatch() {
    // Arrange
    purchaseOrderRepository.save(createOrder());

    Long absentProductId = product.getId() + 1;

    // Act
    List<PurchaseOrder> result = purchaseOrderRepository.findByProductId(absentProductId);

    // Assert
    assertEquals(List.of(), result);
  }

  @Test
  void findByStatusShouldReturnListOfPurchaseOrders() {
    //Arrange
    PurchaseOrder statusOrder = createOrder();

    purchaseOrderRepository.save(statusOrder);

    //Act
    List<PurchaseOrder> result = purchaseOrderRepository.findByStatus(statusOrder.getStatus());

    //Assert
    assertEquals(1, result.size());
    assertEquals(statusOrder.getId(), result.get(0).getId());
    assertEquals(statusOrder.getStatus(), result.get(0).getStatus());
  }

  @Test
  void findByStatusShouldReturnEmptyListWhenNoStatusMatch() {
    //Arrange
    PurchaseOrder statusOrder = createOrder();

    purchaseOrderRepository.save(statusOrder);

    //Act
    List<PurchaseOrder> result = purchaseOrderRepository.findByStatus(PurchaseOrderStatus.CANCELLED);

    //Assert
    assertEquals(0, result.size());
  }

  @Test
  void findByRecommendationIdShouldReturnPurchaseOrders() {
    //Arrange
    PurchaseOrder purchaseOrder = createOrder();

    purchaseOrderRepository.save(purchaseOrder);

    //Act
    Optional<PurchaseOrder> result = purchaseOrderRepository
      .findByRecommendationId(purchaseOrder.getRecommendation().getId());

    //Assert
    assertTrue(result.isPresent());
    assertEquals(purchaseOrder.getId(), result.get().getId());
  }

  @Test
  void findByRecommendationIdShouldReturnEmptyWhenNoOrdersMatch() {
    // Arrange
    purchaseOrderRepository.save(createOrder());
    Long absentRecommendationId = replenishmentRecommendation.getId() + 1;

    // Act
    Optional<PurchaseOrder> result =
      purchaseOrderRepository.findByRecommendationId(absentRecommendationId);

    // Assert
    assertTrue(result.isEmpty());
  }

  @Test
  void existsByRecommendationIdShouldReturnTrueWhenOrderExists() {
    // Arrange
    purchaseOrderRepository.save(createOrder());

    // Act
    boolean result = purchaseOrderRepository
      .existsByRecommendationId(replenishmentRecommendation.getId());

    // Assert
    assertTrue(result);
  }

  @Test
  void existsByRecommendationIdShouldReturnFalseWhenRecommendationHasNoOrder() {
    // Arrange
    // Act
    boolean result = purchaseOrderRepository
      .existsByRecommendationId(replenishmentRecommendation.getId());

    // Assert
    assertEquals(false, result);
  }

  private PurchaseOrder createOrder() {
    PurchaseOrder order = new PurchaseOrder();

    order.setRecommendation(replenishmentRecommendation);
    order.setProduct(product);
    order.setQuantity(100);
    order.setStatus(PurchaseOrderStatus.CREATED);
    order.setCreatedAt(LocalDateTime.now());

    return order;
  }
}
