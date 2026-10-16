package com.skr1l.backend.purchaseOrder;

import com.skr1l.backend.exception.ReplenishmentRecommendationExceptions.ReplenishmentRecommendationIsNotFoundException;
import com.skr1l.backend.exception.purchaseOrderExceptions.PurchaseOrderAlreadyExistsException;
import com.skr1l.backend.exception.purchaseOrderExceptions.PurchaseOrderIsNotFoundException;
import com.skr1l.backend.product.Product;
import com.skr1l.backend.purchaseOrder.dto.PurchaseOrderRequestDto;
import com.skr1l.backend.purchaseOrder.dto.PurchaseOrderResponseDto;
import com.skr1l.backend.purchaseOrder.enums.PurchaseOrderStatus;
import com.skr1l.backend.purchaseOrder.repository.PurchaseOrderRepository;
import com.skr1l.backend.purchaseOrder.service.PurchaseOrderServiceImpl;
import com.skr1l.backend.replenishmentRecommendation.ReplenishmentRecommendation;
import com.skr1l.backend.replenishmentRecommendation.enums.RecommendationStatus;
import com.skr1l.backend.replenishmentRecommendation.repository.ReplenishmentRecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PurchaseOrderServiceTest {


  @Mock
  private ReplenishmentRecommendationRepository replenishmentRecommendationRepository;

  @Mock
  private PurchaseOrderRepository purchaseOrderRepository;

  @InjectMocks
  private PurchaseOrderServiceImpl purchaseOrderService;

  private PurchaseOrder purchaseOrder;

  private Product product;

  private ReplenishmentRecommendation recommendationEntity;

  @BeforeEach
  void setUp() {
    product = new Product();
    product.setId(10L);
    product.setName("product");
    product.setSku("productSku");

    recommendationEntity = new ReplenishmentRecommendation();
    recommendationEntity.setId(20L);
    recommendationEntity.setProduct(product);
    recommendationEntity.setRecommendedQuantity(100);
    recommendationEntity.setRequiredBy(LocalDate.MAX);
    recommendationEntity.setReason("reason");
    recommendationEntity.setStatus(RecommendationStatus.NEW);
    recommendationEntity.setCreatedAt(LocalDateTime.now());

    purchaseOrder = new PurchaseOrder();
    purchaseOrder.setId(30L);
    purchaseOrder.setRecommendation(recommendationEntity);
    purchaseOrder.setProduct(product);
    purchaseOrder.setQuantity(100);
    purchaseOrder.setStatus(PurchaseOrderStatus.CREATED);
    purchaseOrder.setCreatedAt(LocalDateTime.now());
  }

  @Test
  public void createPurchaseShouldReturnIdWhenDataIsValid() {
    //Arrange
    PurchaseOrderRequestDto dto = new PurchaseOrderRequestDto(20L,100);

    when(replenishmentRecommendationRepository.findById(dto.recommendationId()))
    .thenReturn(Optional.of(recommendationEntity));

    when(purchaseOrderRepository.save(any(PurchaseOrder.class)))
      .thenAnswer(invocation -> {
        PurchaseOrder purchaseOrder = invocation.getArgument(0);
        purchaseOrder.setId(30L);
        return purchaseOrder;
      });

    //Act
    Long result = purchaseOrderService.createPurchaseOrder(dto);

    //Assert
    assertEquals(30L, result);
    ArgumentCaptor<PurchaseOrder> captor = ArgumentCaptor.forClass(PurchaseOrder.class);
    verify(purchaseOrderRepository).save(captor.capture());

    PurchaseOrder saved = captor.getValue();
    assertEquals(100, saved.getQuantity());
    assertEquals(PurchaseOrderStatus.CREATED, saved.getStatus());
    assertNotNull(saved.getCreatedAt());
    assertSame(recommendationEntity, saved.getRecommendation());
    assertSame(product, saved.getProduct());
  }

  @Test
  public void createPurchaseShouldThrowExceptionWhenRecommendationIdAlreadyExists() {
    //Arrange
    PurchaseOrderRequestDto dto = new PurchaseOrderRequestDto(10L,10);

    when(purchaseOrderRepository.existsByRecommendationId(10L)).thenReturn(true);

    //Act + Assert
    assertThrows(PurchaseOrderAlreadyExistsException.class,
      () -> purchaseOrderService.createPurchaseOrder(dto));
    verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
  }

  @Test
  public void createPurchaseShouldThrowExceptionWhenRecommendationIsNull() {
    //Arrange
    PurchaseOrderRequestDto dto = new PurchaseOrderRequestDto(134L,10);

    when(purchaseOrderRepository.existsByRecommendationId(134L))
      .thenReturn(false);

    //Act + Assert
    assertThrows(ReplenishmentRecommendationIsNotFoundException.class,
      () -> purchaseOrderService.createPurchaseOrder(dto));
    verify(purchaseOrderRepository, never()).save(any(PurchaseOrder.class));
  }

  @Test
  public void getPurchaseOrderByIdShouldReturnIdWhenDataIsValid() {
    //Arrange
    when(purchaseOrderRepository.findById(30L))
      .thenReturn(Optional.of(purchaseOrder));

    //Act
    PurchaseOrderResponseDto result = purchaseOrderService.getPurchaseOrderById(30L);

    //Assert
    assertEquals(30L, result.id());
    assertEquals(10L, result.productId());
    assertEquals(20L, result.recommendationId());
    assertEquals(100, result.quantity());
    assertEquals(PurchaseOrderStatus.CREATED, result.status());
  }

  @Test
  public void getPurchaseOrderByIdShouldThrowExceptionWhenPurchaseOrderIdDoesNotExist() {
    //Arrange
    when(purchaseOrderRepository.findById(30L))
      .thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(PurchaseOrderIsNotFoundException.class, () -> purchaseOrderService.getPurchaseOrderById(30L));
  }

  @Test
  public void getPurchaseOrderByRecommendationIdShouldReturnIdWhenDataIsValid() {
    //Arrange
    when(purchaseOrderRepository.findByRecommendationId(20L))
      .thenReturn(Optional.of(purchaseOrder));

    //Act
    PurchaseOrderResponseDto result = purchaseOrderService.getPurchaseOrderByRecommendationId(20L);

    //Assert
    assertEquals(30L, result.id());
    assertEquals(10L, result.productId());
    assertEquals(20L, result.recommendationId());
  }

  @Test
  public void getPurchaseOrderByRecommendationIdShouldThrowExceptionWhenPurchaseOrderIdDoesNotExist() {
    //Arrange
    when(purchaseOrderRepository.findByRecommendationId(20L))
      .thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(PurchaseOrderIsNotFoundException.class,
      () -> purchaseOrderService.getPurchaseOrderByRecommendationId(20L));
  }

  @Test
  public void getAllPurchaseOrdersShouldReturnAllPurchaseOrdersWhenDataIsValid() {
    //Arrange
    when(purchaseOrderRepository.findAll())
      .thenReturn(List.of(purchaseOrder));

    //Act
    List<PurchaseOrderResponseDto> result = purchaseOrderService.getAllPurchaseOrders();

    //Assert
    assertEquals(1, result.size());
    assertEquals(100, result.getFirst().quantity());
  }

  @Test
  public void getPurchaseOrdersByProductIdShouldReturnAllPurchaseOrdersWhenDataIsValid() {
    //Arrange
    when(purchaseOrderRepository.findByProductId(10L))
    .thenReturn(List.of(purchaseOrder));

    //Act
    List<PurchaseOrderResponseDto> result = purchaseOrderService.getPurchaseOrdersByProductId(10L);

    //Assert
    assertEquals(1, result.size());
    assertEquals(10L, result.getFirst().productId());
  }

  @Test
  public void getPurchaseOrdersByProductIdShouldReturnEmptyListWhenNoOrdersMatch() {
    //Arrange
    when(purchaseOrderRepository.findByProductId(10L))
      .thenReturn(List.of());

    //Act
    List<PurchaseOrderResponseDto> result =
      purchaseOrderService.getPurchaseOrdersByProductId(10L);

    //Assert
    assertEquals(List.of(), result);
  }

  @Test
  public void getPurchaseOrdersByStatusShouldReturnAllPurchaseOrdersWhenDataIsValid() {
    //Arrange
    when(purchaseOrderRepository.findByStatus(PurchaseOrderStatus.CREATED))
    .thenReturn(List.of(purchaseOrder));

    //Act
    List<PurchaseOrderResponseDto> result =
      purchaseOrderService.getPurchaseOrdersByStatus(PurchaseOrderStatus.CREATED);

    //Assert
    assertEquals(1, result.size());
    assertEquals(PurchaseOrderStatus.CREATED, result.getFirst().status());
  }

  @Test
  public void getPurchaseOrdersByStatusShouldReturnEmptyListWhenNoOrdersMatch() {
    //Arrange
    when(purchaseOrderRepository.findByStatus(PurchaseOrderStatus.CREATED))
      .thenReturn(List.of());

    //Act
    List<PurchaseOrderResponseDto> result =
      purchaseOrderService.getPurchaseOrdersByStatus(PurchaseOrderStatus.CREATED);

    //Assert
    assertEquals(List.of(), result);
  }

  @Test
  public void markAsOrderedShouldMakePurchaseOrderStatusAsOrderedWhenDataIsValid() {
    //Arrange
    when(purchaseOrderRepository.findById(30L))
    .thenReturn(Optional.of(purchaseOrder));

    //Act
    purchaseOrderService.markAsOrdered(30L);

    //Assert
    assertEquals(PurchaseOrderStatus.ORDERED, purchaseOrder.getStatus());
  }

  @Test
  public void markAsOrderedShouldThrowExceptionWhenPurchaseOrderDoesNotExist() {
    //Arrange
    when(purchaseOrderRepository.findById(30L))
      .thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(PurchaseOrderIsNotFoundException.class,
      () -> purchaseOrderService.markAsOrdered(30L));
  }

  @Test
  public void markAsReceivedShouldMakePurchaseOrderStatusAsReceivedWhenDataIsValid() {
    //Arrange
    when(purchaseOrderRepository.findById(30L))
      .thenReturn(Optional.of(purchaseOrder));

    //Act
    purchaseOrderService.markAsReceived(30L);

    //Assert
    assertEquals(PurchaseOrderStatus.RECEIVED, purchaseOrder.getStatus());
  }

  @Test
  public void markAsReceivedShouldThrowExceptionWhenPurchaseOrderDoesNotExist() {
    //Arrange
    when(purchaseOrderRepository.findById(30L))
      .thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(PurchaseOrderIsNotFoundException.class,
      () -> purchaseOrderService.markAsReceived(30L));
  }

  @Test
  public void cancelPurchaseOrderShouldMakePurchaseOrderStatusAsCancelledWhenDataIsValid() {
    //Arrange
    when(purchaseOrderRepository.findById(30L))
      .thenReturn(Optional.of(purchaseOrder));

    //Act
    purchaseOrderService.cancelPurchaseOrder(30L);

    //Assert
    assertEquals(PurchaseOrderStatus.CANCELLED, purchaseOrder.getStatus());
  }

  @Test
  public void cancelPurchaseOrderShouldThrowExceptionWhenPurchaseOrderDoesNotExist() {
    //Arrange
    when(purchaseOrderRepository.findById(30L))
      .thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(PurchaseOrderIsNotFoundException.class,
      () -> purchaseOrderService.cancelPurchaseOrder(30L));
  }

  @Test
  public void deletePurchaseOrderShouldDeletePurchaseOrderWhenDataIsValid() {
    //Arrange
    when(purchaseOrderRepository.findById(30L))
    .thenReturn(Optional.of(purchaseOrder));

    //Act
    purchaseOrderService.deletePurchaseOrder(30L);

    //Assert
    verify(purchaseOrderRepository).delete(purchaseOrder);
  }

  @Test
  public void deletePurchaseOrderShouldThrowExceptionWhenPurchaseOrderDoesNotExist() {
    //Arrange
    when(purchaseOrderRepository.findById(30L))
      .thenReturn(Optional.empty());

    //act + Assert
    assertThrows(PurchaseOrderIsNotFoundException.class,
      () -> purchaseOrderService.deletePurchaseOrder(30L));
    verify(purchaseOrderRepository, never()).delete(any(PurchaseOrder.class));
  }

}
