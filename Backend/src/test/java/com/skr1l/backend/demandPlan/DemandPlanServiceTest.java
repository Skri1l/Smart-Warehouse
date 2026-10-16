package com.skr1l.backend.demandPlan;

import com.skr1l.backend.demandPlan.dto.*;
import com.skr1l.backend.demandPlan.repository.DemandPlanRepository;
import com.skr1l.backend.demandPlan.service.DemandPlanServiceImpl;
import com.skr1l.backend.exception.demandPlanExceptions.DemandPlanIsNotFoundException;
import com.skr1l.backend.exception.productExceptions.ProductIsNotFoundException;
import com.skr1l.backend.product.Product;
import com.skr1l.backend.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DemandPlanServiceTest {

  @Mock
  private DemandPlanRepository repository;

  @Mock
  private ProductRepository productRepository;

  @InjectMocks
  private DemandPlanServiceImpl service;

  private Product product;
  private DemandPlan plan;
  private DemandPlanRequestDto request;
  private LocalDate date;

  @BeforeEach
  public void setup() {
    date = LocalDate.now().plusWeeks(1);

    product = new Product();
    product.setId(10L);

    plan = new DemandPlan();
    plan.setId(20L);
    plan.setProduct(product);
    plan.setRequiredDate(date);
    plan.setRequiredQuantity(100);

    request = new DemandPlanRequestDto(10L, date, 100);
  }

  @Test
  public void createDemandPlanShouldReturnIdAndSaveCorrectFields() {
    //Arrange
    when(productRepository.findById(10L))
      .thenReturn(Optional.of(product));

    when(repository.save(any(DemandPlan.class)))
      .thenAnswer(invocation -> {
        DemandPlan saved = invocation.getArgument(0);
        saved.setId(20L);
        return saved;
      });

    //Act
    Long result = service.createDemandPlan(request);

    //Assert
    assertEquals(20L, result);

    ArgumentCaptor<DemandPlan> captor =
      ArgumentCaptor.forClass(DemandPlan.class);

    verify(repository).save(captor.capture());

    DemandPlan saved = captor.getValue();

    assertSame(product, saved.getProduct());
    assertEquals(date, saved.getRequiredDate());
    assertEquals(100, saved.getRequiredQuantity());
  }

  @Test
  public void createDemandPlanShouldThrowWhenProductDoesNotExist() {
    //Arrange
    when(productRepository.findById(10L))
      .thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(ProductIsNotFoundException.class,
      () -> service.createDemandPlan(request));

    //Assert
    verify(repository, never()).save(any(DemandPlan.class));
  }

  @Test
  public void getDemandPlanByIdShouldReturnDto() {
    //Arrange
    when(repository.findById(20L))
      .thenReturn(Optional.of(plan));

    //Act
    DemandPlanResponseDto result = service.getDemandPlanById(20L);

    //Assert
    assertEquals(expectedDto(), result);
  }

  @Test
  public void getDemandPlanByIdShouldThrowWhenPlanDoesNotExist() {
    //Arrange
    when(repository.findById(20L))
      .thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(DemandPlanIsNotFoundException.class,
      () -> service.getDemandPlanById(20L));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getAllDemandPlansShouldReturnMappedList(boolean empty) {
    //Arrange
    when(repository.findAll())
      .thenReturn(empty ? List.of() : List.of(plan));

    //Act
    List<DemandPlanResponseDto> result = service.getAllDemandPlans();

    //Assert
    assertEquals(empty ? List.of() : List.of(expectedDto()), result);
    verify(repository).findAll();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getDemandPlansByProductIdShouldReturnMappedList(boolean empty) {
    //Arrange
    when(repository.findByProductId(10L))
      .thenReturn(empty ? List.of() : List.of(plan));

    //Act
    List<DemandPlanResponseDto> result =
      service.getDemandPlansByProductId(10L);

    //Assert
    assertEquals(empty ? List.of() : List.of(expectedDto()), result);
    verify(repository).findByProductId(10L);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getDemandPlansByRequiredDateShouldReturnMappedList(boolean empty) {
    //Arrange
    when(repository.findByRequiredDate(date))
      .thenReturn(empty ? List.of() : List.of(plan));

    //Act
    List<DemandPlanResponseDto> result =
      service.getDemandPlansByRequiredDate(date);

    //Assert
    assertEquals(empty ? List.of() : List.of(expectedDto()), result);
    verify(repository).findByRequiredDate(date);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getDemandPlansByDateRangeShouldReturnMappedList(boolean empty) {
    //Arrange
    LocalDate to = date.plusDays(2);

    when(repository.findByRequiredDateBetween(date, to))
      .thenReturn(empty ? List.of() : List.of(plan));

    //Act
    List<DemandPlanResponseDto> result =
      service.getDemandPlansByDateRange(date, to);

    //Assert
    assertEquals(empty ? List.of() : List.of(expectedDto()), result);
    verify(repository).findByRequiredDateBetween(date, to);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getFutureDemandPlansShouldUseCurrentDate(boolean empty) {
    //Arrange
    LocalDate before = LocalDate.now();

    when(repository.findByRequiredDateAfter(any(LocalDate.class)))
      .thenReturn(empty ? List.of() : List.of(plan));

    //Act
    List<DemandPlanResponseDto> result = service.getFutureDemandPlans();
    LocalDate after = LocalDate.now();

    //Assert
    assertEquals(empty ? List.of() : List.of(expectedDto()), result);

    ArgumentCaptor<LocalDate> captor =
      ArgumentCaptor.forClass(LocalDate.class);

    verify(repository).findByRequiredDateAfter(captor.capture());

    assertFalse(captor.getValue().isBefore(before));
    assertFalse(captor.getValue().isAfter(after));
  }

  @Test
  public void updateDemandPlanShouldUpdateFieldsAndReturnDto() {
    //Arrange
    Product replacement = new Product();
    replacement.setId(11L);

    DemandPlanRequestDto update =
      new DemandPlanRequestDto(11L, date.plusDays(2), 200);

    when(repository.findById(20L))
      .thenReturn(Optional.of(plan));

    when(productRepository.findById(11L))
      .thenReturn(Optional.of(replacement));

    //Act
    DemandPlanResponseDto result =
      service.updateDemandPlan(20L, update);

    //Assert
    assertSame(replacement, plan.getProduct());
    assertEquals(update.requiredDate(), plan.getRequiredDate());
    assertEquals(200, plan.getRequiredQuantity());

    assertEquals(
      new DemandPlanResponseDto(20L, 11L, update.requiredDate(), 200),
      result
    );
  }

  @Test
  public void updateDemandPlanShouldThrowWhenPlanDoesNotExist() {
    //Arrange
    when(repository.findById(20L))
      .thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(DemandPlanIsNotFoundException.class,
      () -> service.updateDemandPlan(20L, request));

    //Assert
    verifyNoInteractions(productRepository);
  }

  @Test
  public void updateDemandPlanShouldNotChangeFieldsWhenProductDoesNotExist() {
    //Arrange
    DemandPlanRequestDto update =
      new DemandPlanRequestDto(99L, date.plusDays(2), 200);

    when(repository.findById(20L))
      .thenReturn(Optional.of(plan));

    when(productRepository.findById(99L))
      .thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(ProductIsNotFoundException.class,
      () -> service.updateDemandPlan(20L, update));

    //Assert
    assertSame(product, plan.getProduct());
    assertEquals(date, plan.getRequiredDate());
    assertEquals(100, plan.getRequiredQuantity());
  }

  @Test
  public void deleteDemandPlanShouldDeleteExistingPlan() {
    //Arrange
    when(repository.findById(20L))
      .thenReturn(Optional.of(plan));

    //Act
    service.deleteDemandPlan(20L);

    //Assert
    verify(repository).delete(plan);
  }

  @Test
  public void deleteDemandPlanShouldThrowWhenPlanDoesNotExist() {
    //Arrange
    when(repository.findById(20L))
      .thenReturn(Optional.empty());

    //Act + Assert
    assertThrows(DemandPlanIsNotFoundException.class,
      () -> service.deleteDemandPlan(20L));

    //Assert
    verify(repository, never()).delete(any(DemandPlan.class));
  }

  private DemandPlanResponseDto expectedDto() {
    return new DemandPlanResponseDto(20L, 10L, date, 100);
  }
}
