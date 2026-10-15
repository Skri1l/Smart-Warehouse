package com.skr1l.backend.ReplenishmentRecommendation;

import com.skr1l.backend.exception.ReplenishmentRecommendationExceptions.ReplenishmentRecommendationIsNotFoundException;
import com.skr1l.backend.exception.productExceptions.ProductIsNotFoundException;
import com.skr1l.backend.product.Product;
import com.skr1l.backend.product.repository.ProductRepository;
import com.skr1l.backend.replenishmentRecommendation.ReplenishmentRecommendation;
import com.skr1l.backend.replenishmentRecommendation.dto.ReplenishmentRecommendationRequestDto;
import com.skr1l.backend.replenishmentRecommendation.dto.ReplenishmentRecommendationResponseDto;
import com.skr1l.backend.replenishmentRecommendation.enums.RecommendationStatus;
import com.skr1l.backend.replenishmentRecommendation.repository.ReplenishmentRecommendationRepository;
import com.skr1l.backend.replenishmentRecommendation.service.ReplenishmentRecommendationServiceImpl;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReplenishmentRecommendationServiceTest {

  @Mock
  private ReplenishmentRecommendationRepository recommendationRepository;

  @Mock
  private ProductRepository productRepository;

  @InjectMocks
  private ReplenishmentRecommendationServiceImpl replenishmentRecommendationServiceImpl;

  private ReplenishmentRecommendation replenishmentRecommendation;

  private Product product;

  @BeforeEach
  public void setup() {
    product = new Product();
    product.setId(10L);
    product.setName("product");
    product.setSku("productSku");

    replenishmentRecommendation = new ReplenishmentRecommendation();
    replenishmentRecommendation.setId(20L);
    replenishmentRecommendation.setProduct(product);
    replenishmentRecommendation.setRecommendedQuantity(100);
    replenishmentRecommendation.setRequiredBy(LocalDate.now().plusWeeks(1));
    replenishmentRecommendation.setReason("reason");
    replenishmentRecommendation.setStatus(RecommendationStatus.NEW);
    replenishmentRecommendation.setCreatedAt(LocalDateTime.now());
  }

  @Test
  public void createRecommendationShouldReturnIdWhenDataIsValid() {
    //Arrange
    ReplenishmentRecommendationRequestDto dto = new ReplenishmentRecommendationRequestDto(
      10L,
      100,
      LocalDate.now().plusWeeks(1),
      "reason"
    );
    when(productRepository.findById(dto.productId()))
      .thenReturn(Optional.of(product));

    when(recommendationRepository.save(any(ReplenishmentRecommendation.class)))
      .thenAnswer(invocation -> {
        ReplenishmentRecommendation replenishmentRecommendation = invocation.getArgument(0);
        replenishmentRecommendation.setId(20L);
        return replenishmentRecommendation;
      });

    //Act
    Long result = replenishmentRecommendationServiceImpl.createRecommendation(dto);

    //Assert
    assertEquals(20L, result);
    ArgumentCaptor<ReplenishmentRecommendation> captor = ArgumentCaptor
      .forClass(ReplenishmentRecommendation.class);
    verify(recommendationRepository).save(captor.capture());

    ReplenishmentRecommendation captured = captor.getValue();
    assertEquals(20L, captured.getId());
    assertEquals(100, captured.getRecommendedQuantity());
    assertEquals("reason", captured.getReason());
    assertEquals(RecommendationStatus.NEW, captured.getStatus());
    assertSame(product, captured.getProduct());
    assertEquals(dto.requiredBy(), captured.getRequiredBy());
    assertNotNull(captured.getCreatedAt());
  }

  @Test
  public void createRecommendationShouldThrowExceptionWhenProductDoesntExist() {
    //Arrange
    ReplenishmentRecommendationRequestDto dto = new ReplenishmentRecommendationRequestDto(
      10L,
      100,
      LocalDate.now().plusWeeks(1),
      "reason"
    );

    when(productRepository.findById(dto.productId()))
      .thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(ProductIsNotFoundException.class,
      () -> replenishmentRecommendationServiceImpl.createRecommendation(dto));
    verify(recommendationRepository, never()).save(any(ReplenishmentRecommendation.class));
  }

  @Test
  public void getRecommendationByIdShouldReturnIdWhenDataIsValid() {
    //Arrange
    when(recommendationRepository.findById(20L))
      .thenReturn(Optional.of(replenishmentRecommendation));

    //Act
    ReplenishmentRecommendationResponseDto result =
      replenishmentRecommendationServiceImpl.getRecommendationById(20L);

    //Assert
    assertEquals(20L, result.id());
    assertEquals(10L, result.productId());
    assertEquals(RecommendationStatus.NEW, result.status());
  }

  @Test
  public void getRecommendationByIdShouldThrowExceptionWhenRecommendationDoesNotExist() {
    //Arrange
    when(recommendationRepository.findById(20L))
      .thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(ReplenishmentRecommendationIsNotFoundException.class,
      () -> replenishmentRecommendationServiceImpl.getRecommendationById(20L));
  }

  @Test
  public void getAllRecommendationsShouldReturnAllRecommendations() {
    //Arrange
    when(recommendationRepository.findAll())
      .thenReturn(List.of(replenishmentRecommendation));

    //Act + Assert
    List<ReplenishmentRecommendationResponseDto> result =
      replenishmentRecommendationServiceImpl.getAllRecommendations();

    assertEquals(1, result.size());
    assertRecommendationDto(result.get(0));
    verify(recommendationRepository).findAll();
  }

  @Test
  public void getAllRecommendationsShouldReturnEmptyListWhenNoRecommendationsExist() {
    //Arrange
    when(recommendationRepository.findAll())
      .thenReturn(List.of());

    //Act + Assert
    List<ReplenishmentRecommendationResponseDto> result =
      replenishmentRecommendationServiceImpl.getAllRecommendations();

    assertTrue(result.isEmpty());
  }

  @Test
  public void getRecommendationsByProductIdShouldReturnMatchingRecommendations() {
    //Arrange
    when(recommendationRepository.findByProductId(10L))
      .thenReturn(List.of(replenishmentRecommendation));

    //Act
    List<ReplenishmentRecommendationResponseDto> result =
      replenishmentRecommendationServiceImpl.getRecommendationsByProductId(10L);

    //Assert
    assertEquals(1, result.size());
    assertRecommendationDto(result.get(0));
    verify(recommendationRepository).findByProductId(10L);
  }

  @Test
  public void getRecommendationsByProductIdShouldReturnEmptyListWhenNoRecommendationsMatch() {
    //Arrange
    when(recommendationRepository.findByProductId(10L))
      .thenReturn(List.of());

    //Act
    List<ReplenishmentRecommendationResponseDto> result =
      replenishmentRecommendationServiceImpl.getRecommendationsByProductId(10L);

    //Assert
    assertTrue(result.isEmpty());
  }

  @Test
  public void getRecommendationsByStatusShouldReturnMatchingRecommendations() {
    //Arrange
    when(recommendationRepository.findByStatus(RecommendationStatus.NEW))
      .thenReturn(List.of(replenishmentRecommendation));

    //Act
    List<ReplenishmentRecommendationResponseDto> result =
      replenishmentRecommendationServiceImpl
        .getRecommendationsByStatus(RecommendationStatus.NEW);

    //Assert
    assertEquals(1, result.size());
    assertRecommendationDto(result.get(0));
    verify(recommendationRepository).findByStatus(RecommendationStatus.NEW);
  }

  @Test
  public void getRecommendationsByStatusShouldReturnEmptyListWhenNoRecommendationsMatch() {
    when(recommendationRepository.findByStatus(RecommendationStatus.APPROVED))
      .thenReturn(List.of());

    List<ReplenishmentRecommendationResponseDto> result =
      replenishmentRecommendationServiceImpl
        .getRecommendationsByStatus(RecommendationStatus.APPROVED);

    assertTrue(result.isEmpty());
  }

  @Test
  public void getRecommendationsByRequiredByShouldReturnMatchingRecommendations() {
    LocalDate requiredBy = replenishmentRecommendation.getRequiredBy();

    when(recommendationRepository.findByRequiredBy(requiredBy))
      .thenReturn(List.of(replenishmentRecommendation));

    List<ReplenishmentRecommendationResponseDto> result =
      replenishmentRecommendationServiceImpl
        .getRecommendationsByRequiredBy(requiredBy);

    assertEquals(1, result.size());
    assertRecommendationDto(result.get(0));
    verify(recommendationRepository).findByRequiredBy(requiredBy);
  }

  @Test
  public void getRecommendationsByRequiredByShouldReturnEmptyListWhenNoRecommendationsMatch() {
    LocalDate requiredBy = replenishmentRecommendation.getRequiredBy();

    when(recommendationRepository.findByRequiredBy(requiredBy))
      .thenReturn(List.of());

    List<ReplenishmentRecommendationResponseDto> result =
      replenishmentRecommendationServiceImpl
        .getRecommendationsByRequiredBy(requiredBy);

    assertTrue(result.isEmpty());
  }

  @Test
  public void getRecommendationsByDateRangeShouldReturnMatchingRecommendations() {
    LocalDate requiredBy = replenishmentRecommendation.getRequiredBy();
    LocalDate from = requiredBy.minusDays(1);
    LocalDate to = requiredBy.plusDays(1);

    when(recommendationRepository.findByRequiredByBetween(from, to))
      .thenReturn(List.of(replenishmentRecommendation));

    List<ReplenishmentRecommendationResponseDto> result =
      replenishmentRecommendationServiceImpl
        .getRecommendationsByDateRange(from, to);

    assertEquals(1, result.size());
    assertRecommendationDto(result.get(0));
    verify(recommendationRepository).findByRequiredByBetween(from, to);
  }

  @Test
  public void getRecommendationsByDateRangeShouldReturnEmptyListWhenNoRecommendationsMatch() {
    LocalDate from = LocalDate.of(2026, 1, 1);
    LocalDate to = LocalDate.of(2026, 1, 31);

    when(recommendationRepository.findByRequiredByBetween(from, to))
      .thenReturn(List.of());

    List<ReplenishmentRecommendationResponseDto> result =
      replenishmentRecommendationServiceImpl
        .getRecommendationsByDateRange(from, to);

    assertTrue(result.isEmpty());
  }

  @Test
  public void updateRecommendationShouldUpdateFieldsAndReturnDto() {
    Product updatedProduct = new Product();
    updatedProduct.setId(11L);

    ReplenishmentRecommendationRequestDto dto =
      new ReplenishmentRecommendationRequestDto(
        11L,
        200,
        replenishmentRecommendation.getRequiredBy().plusDays(3),
        "updated reason"
      );

    replenishmentRecommendation.setStatus(RecommendationStatus.APPROVED);
    LocalDateTime originalCreatedAt = replenishmentRecommendation.getCreatedAt();

    when(recommendationRepository.findById(20L))
      .thenReturn(Optional.of(replenishmentRecommendation));
    when(productRepository.findById(11L))
      .thenReturn(Optional.of(updatedProduct));

    ReplenishmentRecommendationResponseDto result =
      replenishmentRecommendationServiceImpl.updateRecommendation(20L, dto);

    assertSame(updatedProduct, replenishmentRecommendation.getProduct());
    assertEquals(200, replenishmentRecommendation.getRecommendedQuantity());
    assertEquals(dto.requiredBy(), replenishmentRecommendation.getRequiredBy());
    assertEquals("updated reason", replenishmentRecommendation.getReason());
    assertEquals(RecommendationStatus.APPROVED, replenishmentRecommendation.getStatus());
    assertEquals(originalCreatedAt, replenishmentRecommendation.getCreatedAt());

    assertRecommendationDto(result);
    verify(productRepository).findById(11L);
  }

  @Test
  public void updateRecommendationShouldThrowExceptionWhenRecommendationDoesNotExist() {
    ReplenishmentRecommendationRequestDto dto =
      new ReplenishmentRecommendationRequestDto(
        10L,
        200,
        replenishmentRecommendation.getRequiredBy(),
        "updated reason"
      );

    when(recommendationRepository.findById(20L))
      .thenReturn(Optional.empty());

    assertThrows(ReplenishmentRecommendationIsNotFoundException.class,
      () -> replenishmentRecommendationServiceImpl.updateRecommendation(20L, dto));

    verifyNoInteractions(productRepository);
    verify(recommendationRepository, never())
      .save(any(ReplenishmentRecommendation.class));
  }

  @Test
  public void updateRecommendationShouldThrowExceptionWhenProductDoesNotExist() {
    LocalDate originalRequiredBy = replenishmentRecommendation.getRequiredBy();

    ReplenishmentRecommendationRequestDto dto =
      new ReplenishmentRecommendationRequestDto(
        99L,
        200,
        originalRequiredBy.plusDays(3),
        "updated reason"
      );

    when(recommendationRepository.findById(20L))
      .thenReturn(Optional.of(replenishmentRecommendation));
    when(productRepository.findById(99L))
      .thenReturn(Optional.empty());

    assertThrows(ProductIsNotFoundException.class,
      () -> replenishmentRecommendationServiceImpl.updateRecommendation(20L, dto));

    assertSame(product, replenishmentRecommendation.getProduct());
    assertEquals(100, replenishmentRecommendation.getRecommendedQuantity());
    assertEquals(originalRequiredBy, replenishmentRecommendation.getRequiredBy());
    assertEquals("reason", replenishmentRecommendation.getReason());

    verify(recommendationRepository, never())
      .save(any(ReplenishmentRecommendation.class));
  }

  @Test
  public void approveRecommendationShouldChangeStatusToApproved() {
    when(recommendationRepository.findById(20L))
      .thenReturn(Optional.of(replenishmentRecommendation));

    replenishmentRecommendationServiceImpl.approveRecommendation(20L);

    assertEquals(RecommendationStatus.APPROVED, replenishmentRecommendation.getStatus());
    verify(recommendationRepository).findById(20L);
  }

  @Test
  public void approveRecommendationShouldThrowExceptionWhenRecommendationDoesNotExist() {
    when(recommendationRepository.findById(20L))
      .thenReturn(Optional.empty());

    assertThrows(ReplenishmentRecommendationIsNotFoundException.class,
      () -> replenishmentRecommendationServiceImpl.approveRecommendation(20L));
  }

  @Test
  public void rejectRecommendationShouldChangeStatusToRejected() {
    when(recommendationRepository.findById(20L))
      .thenReturn(Optional.of(replenishmentRecommendation));

    replenishmentRecommendationServiceImpl.rejectRecommendation(20L);

    assertEquals(RecommendationStatus.REJECTED, replenishmentRecommendation.getStatus());
    verify(recommendationRepository).findById(20L);
  }

  @Test
  public void rejectRecommendationShouldThrowExceptionWhenRecommendationDoesNotExist() {
    when(recommendationRepository.findById(20L))
      .thenReturn(Optional.empty());

    assertThrows(ReplenishmentRecommendationIsNotFoundException.class,
      () -> replenishmentRecommendationServiceImpl.rejectRecommendation(20L));
  }

  @Test
  public void markRecommendationAsOrderedShouldChangeStatusToOrdered() {
    when(recommendationRepository.findById(20L))
      .thenReturn(Optional.of(replenishmentRecommendation));

    replenishmentRecommendationServiceImpl.markRecommendationAsOrdered(20L);

    assertEquals(RecommendationStatus.ORDERED, replenishmentRecommendation.getStatus());
    verify(recommendationRepository).findById(20L);
  }

  @Test
  public void markRecommendationAsOrderedShouldThrowExceptionWhenRecommendationDoesNotExist() {
    when(recommendationRepository.findById(20L))
      .thenReturn(Optional.empty());

    assertThrows(ReplenishmentRecommendationIsNotFoundException.class,
      () -> replenishmentRecommendationServiceImpl.markRecommendationAsOrdered(20L));
  }

  @Test
  public void completeRecommendationShouldChangeStatusToCompleted() {
    when(recommendationRepository.findById(20L))
      .thenReturn(Optional.of(replenishmentRecommendation));

    replenishmentRecommendationServiceImpl.completeRecommendation(20L);

    assertEquals(RecommendationStatus.COMPLETED, replenishmentRecommendation.getStatus());
    verify(recommendationRepository).findById(20L);
  }

  @Test
  public void completeRecommendationShouldThrowExceptionWhenRecommendationDoesNotExist() {
    when(recommendationRepository.findById(20L))
      .thenReturn(Optional.empty());

    assertThrows(ReplenishmentRecommendationIsNotFoundException.class,
      () -> replenishmentRecommendationServiceImpl.completeRecommendation(20L));
  }

  @Test
  public void deleteRecommendationShouldDeleteExistingRecommendation() {
    when(recommendationRepository.findById(20L))
      .thenReturn(Optional.of(replenishmentRecommendation));

    replenishmentRecommendationServiceImpl.deleteRecommendation(20L);

    verify(recommendationRepository).delete(replenishmentRecommendation);
  }

  @Test
  public void deleteRecommendationShouldThrowExceptionWhenRecommendationDoesNotExist() {
    when(recommendationRepository.findById(20L))
      .thenReturn(Optional.empty());

    assertThrows(ReplenishmentRecommendationIsNotFoundException.class,
      () -> replenishmentRecommendationServiceImpl.deleteRecommendation(20L));

    verify(recommendationRepository, never())
      .delete(any(ReplenishmentRecommendation.class));
  }

  @Test
  public void serviceMethodsShouldRejectNullArgumentsBeforeCallingRepositories() {
    ReplenishmentRecommendationRequestDto dto =
      new ReplenishmentRecommendationRequestDto(
        10L,
        100,
        replenishmentRecommendation.getRequiredBy(),
        "reason"
      );
    LocalDate date = replenishmentRecommendation.getRequiredBy();

    assertThrows(NullPointerException.class,
      () -> replenishmentRecommendationServiceImpl.createRecommendation(null));

    assertThrows(NullPointerException.class,
      () -> replenishmentRecommendationServiceImpl.getRecommendationById(null));

    assertThrows(NullPointerException.class,
      () -> replenishmentRecommendationServiceImpl
        .getRecommendationsByProductId(null));

    assertThrows(NullPointerException.class,
      () -> replenishmentRecommendationServiceImpl
        .getRecommendationsByStatus(null));

    assertThrows(NullPointerException.class,
      () -> replenishmentRecommendationServiceImpl
        .getRecommendationsByRequiredBy(null));

    assertThrows(NullPointerException.class,
      () -> replenishmentRecommendationServiceImpl
        .getRecommendationsByDateRange(null, date));

    assertThrows(NullPointerException.class,
      () -> replenishmentRecommendationServiceImpl
        .getRecommendationsByDateRange(date, null));

    assertThrows(NullPointerException.class,
      () -> replenishmentRecommendationServiceImpl
        .updateRecommendation(null, dto));

    assertThrows(NullPointerException.class,
      () -> replenishmentRecommendationServiceImpl
        .updateRecommendation(20L, null));

    assertThrows(NullPointerException.class,
      () -> replenishmentRecommendationServiceImpl.approveRecommendation(null));

    assertThrows(NullPointerException.class,
      () -> replenishmentRecommendationServiceImpl.rejectRecommendation(null));

    assertThrows(NullPointerException.class,
      () -> replenishmentRecommendationServiceImpl.markRecommendationAsOrdered(null));

    assertThrows(NullPointerException.class,
      () -> replenishmentRecommendationServiceImpl.completeRecommendation(null));

    assertThrows(NullPointerException.class,
      () -> replenishmentRecommendationServiceImpl.deleteRecommendation(null));

    verifyNoInteractions(recommendationRepository, productRepository);
  }

  private void assertRecommendationDto(ReplenishmentRecommendationResponseDto result) {
    assertEquals(replenishmentRecommendation.getId(), result.id());
    assertEquals(replenishmentRecommendation.getProduct().getId(), result.productId());
    assertEquals(
      replenishmentRecommendation.getRecommendedQuantity(),
      result.recommendedQuantity()
    );
    assertEquals(replenishmentRecommendation.getRequiredBy(), result.requiredBy());
    assertEquals(replenishmentRecommendation.getReason(), result.reason());
    assertEquals(replenishmentRecommendation.getStatus(), result.status());
    assertEquals(replenishmentRecommendation.getCreatedAt(), result.createdAt());
  }
}
