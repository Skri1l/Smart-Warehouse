package com.skr1l.backend.purchaseOrder;

import com.skr1l.backend.exception.GlobalExceptionHandler;
import com.skr1l.backend.exception.ReplenishmentRecommendationExceptions.ReplenishmentRecommendationIsNotFoundException;
import com.skr1l.backend.exception.purchaseOrderExceptions.PurchaseOrderAlreadyExistsException;
import com.skr1l.backend.exception.purchaseOrderExceptions.PurchaseOrderIsNotFoundException;
import com.skr1l.backend.purchaseOrder.controller.PurchaseOrderController;
import com.skr1l.backend.purchaseOrder.dto.PurchaseOrderRequestDto;
import com.skr1l.backend.purchaseOrder.dto.PurchaseOrderResponseDto;
import com.skr1l.backend.purchaseOrder.enums.PurchaseOrderStatus;
import com.skr1l.backend.purchaseOrder.service.PurchaseOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.LocalDateTime;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class PurchaseOrderControllerTest {

  @Mock
  private PurchaseOrderService purchaseOrderService;

  private MockMvc mockMvc;

  private PurchaseOrderResponseDto dto;

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders
      .standaloneSetup(new PurchaseOrderController(purchaseOrderService))
      .setControllerAdvice(new GlobalExceptionHandler())
      .build();

    dto = new PurchaseOrderResponseDto(
      30L,
      20L,
      10L,
      100,
      PurchaseOrderStatus.CREATED,
      LocalDateTime.now());
  }

  @Test
  public void getPurchaseOrderByIdShouldReturnPurchaseOrderId() throws Exception {
    //Arrange
    when(purchaseOrderService.getPurchaseOrderById(30L))
      .thenReturn(dto);

    //Act + Assert
    mockMvc.perform(get("/purchase-orders/30"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(30))
      .andExpect(jsonPath("$.recommendationId").value(20))
      .andExpect(jsonPath("$.productId").value(10))
      .andExpect(jsonPath("$.quantity").value(100))
      .andExpect(jsonPath("$.status").value(PurchaseOrderStatus.CREATED.toString()));
  }

  @Test
  public void getPurchaseOrderByIdShouldThrowExceptionIfOrderIsInvalid() throws Exception {
    //Arrange
    when(purchaseOrderService.getPurchaseOrderById(30L))
      .thenThrow(PurchaseOrderIsNotFoundException.class);

    //Act + Assert
    mockMvc.perform(get("/purchase-orders/30"))
      .andExpect(status().isNotFound());
  }

  @Test
  public void getAllPurchaseOrdersShouldReturnAllPurchaseOrders() throws Exception {
    //Arrange
    when(purchaseOrderService.getAllPurchaseOrders())
      .thenReturn(List.of(dto));

    //Act + Assert
    mockMvc.perform(get("/purchase-orders"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].id").value(30))
      .andExpect(jsonPath("$[0].recommendationId").value(20))
      .andExpect(jsonPath("$[0].productId").value(10));
  }

  @Test
  public void getAllPurchaseOrdersShouldReturnNothingIfHasNoObjects() throws Exception {
    //Arrange
    when(purchaseOrderService.getAllPurchaseOrders())
    .thenReturn(List.of());

    //Act + Assert
    mockMvc.perform(get("/purchase-orders"))
      .andExpect(status().isOk())
      .andExpect(content().json("[]"));
  }

  @Test
  public void getPurchaseOrdersByProductIdShouldReturnAllPurchaseOrders() throws Exception {
    //Arrange
    when(purchaseOrderService.getPurchaseOrdersByProductId(10L))
      .thenReturn(List.of(dto));

    //Act + Assert
    mockMvc.perform(get("/purchase-orders/product/10"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].id").value(30))
      .andExpect(jsonPath("$[0].recommendationId").value(20))
      .andExpect(jsonPath("$[0].productId").value(10))
      .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  public void getPurchaseOrdersByProductIdShouldReturnNothingIfHasNoObjects() throws Exception {
    //Arrange
    when(purchaseOrderService.getPurchaseOrdersByProductId(10L))
      .thenReturn(List.of());

    //Act + Assert
    mockMvc.perform(get("/purchase-orders/product/10"))
      .andExpect(status().isOk())
      .andExpect(content().json("[]"))
      .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  public void createPurchaseOrderShouldReturnCreatedAndId() throws Exception {
    PurchaseOrderRequestDto request = new PurchaseOrderRequestDto(20L, 100);

    when(purchaseOrderService.createPurchaseOrder(request))
      .thenReturn(30L);

    mockMvc.perform(post("/purchase-orders")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                {
                    "recommendationId": 20,
                    "quantity": 100
                }
                """))
      .andExpect(status().isCreated())
      .andExpect(content().string("30"));

    verify(purchaseOrderService).createPurchaseOrder(request);
  }

  @Test
  public void createPurchaseOrderShouldReturnBadRequestWhenDataIsInvalid()
    throws Exception {
    List<String> invalidRequests = List.of(
      """
      {"recommendationId": null, "quantity": 100}
      """,
      """
      {"recommendationId": 20, "quantity": null}
      """,
      """
      {"recommendationId": 20, "quantity": 0}
      """,
      """
      {"recommendationId": 20, "quantity": -1}
      """
    );

    for (String json : invalidRequests) {
      mockMvc.perform(post("/purchase-orders")
          .contentType(MediaType.APPLICATION_JSON)
          .content(json))
        .andExpect(status().isBadRequest());
    }

    verifyNoInteractions(purchaseOrderService);
  }

  @Test
  public void createPurchaseOrderShouldReturnNotFoundWhenRecommendationDoesNotExist()
    throws Exception {
    PurchaseOrderRequestDto request = new PurchaseOrderRequestDto(20L, 100);

    when(purchaseOrderService.createPurchaseOrder(request))
      .thenThrow(new ReplenishmentRecommendationIsNotFoundException(
        "Recommendation not found"));

    mockMvc.perform(post("/purchase-orders")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                {"recommendationId": 20, "quantity": 100}
                """))
      .andExpect(status().isNotFound());
  }

  @Test
  public void createPurchaseOrderShouldReturnConflictWhenOrderAlreadyExists()
    throws Exception {
    PurchaseOrderRequestDto request = new PurchaseOrderRequestDto(20L, 100);

    when(purchaseOrderService.createPurchaseOrder(request))
      .thenThrow(new PurchaseOrderAlreadyExistsException(
        "Purchase order already exists"));

    mockMvc.perform(post("/purchase-orders")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                {"recommendationId": 20, "quantity": 100}
                """))
      .andExpect(status().isConflict());
  }

  @Test
  public void getPurchaseOrderByRecommendationIdShouldReturnPurchaseOrder()
    throws Exception {
    when(purchaseOrderService.getPurchaseOrderByRecommendationId(20L))
      .thenReturn(dto);

    mockMvc.perform(get("/purchase-orders/recommendation/20"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(30))
      .andExpect(jsonPath("$.recommendationId").value(20))
      .andExpect(jsonPath("$.productId").value(10))
      .andExpect(jsonPath("$.quantity").value(100))
      .andExpect(jsonPath("$.status").value("CREATED"));

    verify(purchaseOrderService).getPurchaseOrderByRecommendationId(20L);
  }

  @Test
  public void getPurchaseOrderByRecommendationIdShouldReturnNotFoundWhenOrderDoesNotExist()
    throws Exception {
    when(purchaseOrderService.getPurchaseOrderByRecommendationId(20L))
      .thenThrow(new PurchaseOrderIsNotFoundException(
        "Purchase order not found"));

    mockMvc.perform(get("/purchase-orders/recommendation/20"))
      .andExpect(status().isNotFound());
  }

  @Test
  public void getPurchaseOrdersByStatusShouldReturnMatchingOrders()
    throws Exception {
    when(purchaseOrderService.getPurchaseOrdersByStatus(PurchaseOrderStatus.CREATED))
      .thenReturn(List.of(dto));

    mockMvc.perform(get("/purchase-orders/status/CREATED"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].id").value(30))
      .andExpect(jsonPath("$[0].status").value("CREATED"));

    verify(purchaseOrderService)
      .getPurchaseOrdersByStatus(PurchaseOrderStatus.CREATED);
  }

  @Test
  public void getPurchaseOrdersByStatusShouldReturnEmptyListWhenNoOrdersMatch()
    throws Exception {
    when(purchaseOrderService.getPurchaseOrdersByStatus(PurchaseOrderStatus.CANCELLED))
      .thenReturn(List.of());

    mockMvc.perform(get("/purchase-orders/status/CANCELLED"))
      .andExpect(status().isOk())
      .andExpect(content().json("[]"));
  }

  @Test
  public void getPurchaseOrdersByStatusShouldReturnBadRequestWhenStatusIsInvalid()
    throws Exception {
    mockMvc.perform(get("/purchase-orders/status/UNKNOWN"))
      .andExpect(status().isBadRequest());

    verifyNoInteractions(purchaseOrderService);
  }

  @Test
  public void markAsOrderedShouldReturnNoContent() throws Exception {
    mockMvc.perform(patch("/purchase-orders/30/ordered"))
      .andExpect(status().isNoContent())
      .andExpect(content().string(""));

    verify(purchaseOrderService).markAsOrdered(30L);
  }

  @Test
  public void markAsOrderedShouldReturnNotFoundWhenOrderDoesNotExist()
    throws Exception {
    doThrow(new PurchaseOrderIsNotFoundException("Purchase order not found"))
      .when(purchaseOrderService).markAsOrdered(30L);

    mockMvc.perform(patch("/purchase-orders/30/ordered"))
      .andExpect(status().isNotFound());
  }

  @Test
  public void markAsReceivedShouldReturnNoContent() throws Exception {
    mockMvc.perform(patch("/purchase-orders/30/received"))
      .andExpect(status().isNoContent())
      .andExpect(content().string(""));

    verify(purchaseOrderService).markAsReceived(30L);
  }

  @Test
  public void markAsReceivedShouldReturnNotFoundWhenOrderDoesNotExist()
    throws Exception {
    doThrow(new PurchaseOrderIsNotFoundException("Purchase order not found"))
      .when(purchaseOrderService).markAsReceived(30L);

    mockMvc.perform(patch("/purchase-orders/30/received"))
      .andExpect(status().isNotFound());
  }

  @Test
  public void cancelPurchaseOrderShouldReturnNoContent() throws Exception {
    mockMvc.perform(patch("/purchase-orders/30/cancel"))
      .andExpect(status().isNoContent())
      .andExpect(content().string(""));

    verify(purchaseOrderService).cancelPurchaseOrder(30L);
  }

  @Test
  public void cancelPurchaseOrderShouldReturnNotFoundWhenOrderDoesNotExist()
    throws Exception {
    doThrow(new PurchaseOrderIsNotFoundException("Purchase order not found"))
      .when(purchaseOrderService).cancelPurchaseOrder(30L);

    mockMvc.perform(patch("/purchase-orders/30/cancel"))
      .andExpect(status().isNotFound());
  }

  @Test
  public void deletePurchaseOrderShouldReturnNoContent() throws Exception {
    mockMvc.perform(delete("/purchase-orders/30"))
      .andExpect(status().isNoContent())
      .andExpect(content().string(""));

    verify(purchaseOrderService).deletePurchaseOrder(30L);
  }

  @Test
  public void deletePurchaseOrderShouldReturnNotFoundWhenOrderDoesNotExist()
    throws Exception {
    doThrow(new PurchaseOrderIsNotFoundException("Purchase order not found"))
      .when(purchaseOrderService).deletePurchaseOrder(30L);

    mockMvc.perform(delete("/purchase-orders/30"))
      .andExpect(status().isNotFound());
  }
}
