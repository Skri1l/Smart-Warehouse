package com.skr1l.backend.demandPlan;

import com.skr1l.backend.demandPlan.controller.DemandPlanController;
import com.skr1l.backend.demandPlan.dto.*;
import com.skr1l.backend.demandPlan.service.DemandPlanService;
import com.skr1l.backend.exception.GlobalExceptionHandler;
import com.skr1l.backend.exception.demandPlanExceptions.DemandPlanIsNotFoundException;
import com.skr1l.backend.exception.productExceptions.ProductIsNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class DemandPlanControllerTest {

  @Mock
  private DemandPlanService service;

  private MockMvc mockMvc;
  private DemandPlanRequestDto request;
  private DemandPlanResponseDto response;
  private LocalDate date;
  private String json;

  @BeforeEach
  public void setup() {
    date = LocalDate.now().plusWeeks(1);
    request = new DemandPlanRequestDto(10L, date, 100);
    response = new DemandPlanResponseDto(20L, 10L, date, 100);

    json = """
            {
                "productId": 10,
                "requiredDate": "%s",
                "requiredQuantity": 100
            }
            """.formatted(date);

    mockMvc = MockMvcBuilders
      .standaloneSetup(new DemandPlanController(service))
      .setControllerAdvice(new GlobalExceptionHandler())
      .build();
  }

  @Test
  public void createDemandPlanShouldReturnCreatedAndId() throws Exception {
    //Arrange
    when(service.createDemandPlan(request)).thenReturn(20L);

    //Act + Assert
    mockMvc.perform(post("/demand-plans")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
      .andExpect(status().isCreated())
      .andExpect(content().string("20"));

    //Assert
    verify(service).createDemandPlan(request);
  }

  @Test
  public void createDemandPlanShouldReturnNotFoundWhenProductDoesNotExist()
    throws Exception {
    //Arrange
    when(service.createDemandPlan(request))
      .thenThrow(new ProductIsNotFoundException("Product not found"));

    //Act + Assert
    mockMvc.perform(post("/demand-plans")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
      .andExpect(status().isNotFound());
  }

  @ParameterizedTest
  @MethodSource("invalidRequests")
  public void createDemandPlanShouldRejectInvalidData(String invalidJson)
    throws Exception {
    //Act + Assert
    mockMvc.perform(post("/demand-plans")
        .contentType(MediaType.APPLICATION_JSON)
        .content(invalidJson))
      .andExpect(status().isBadRequest());

    //Assert
    verifyNoInteractions(service);
  }

  @Test
  public void getDemandPlanByIdShouldReturnDto() throws Exception {
    //Arrange
    when(service.getDemandPlanById(20L)).thenReturn(response);

    //Act + Assert
    mockMvc.perform(get("/demand-plans/20"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(20))
      .andExpect(jsonPath("$.productId").value(10))
      .andExpect(jsonPath("$.requiredDate").value(date.toString()))
      .andExpect(jsonPath("$.requiredQuantity").value(100));
  }

  @Test
  public void getDemandPlanByIdShouldReturnNotFound() throws Exception {
    //Arrange
    when(service.getDemandPlanById(20L))
      .thenThrow(new DemandPlanIsNotFoundException("Plan not found"));

    //Act + Assert
    mockMvc.perform(get("/demand-plans/20"))
      .andExpect(status().isNotFound());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getAllDemandPlansShouldReturnList(boolean empty) throws Exception {
    //Arrange
    when(service.getAllDemandPlans())
      .thenReturn(empty ? List.of() : List.of(response));

    //Act + Assert
    assertList(get("/demand-plans"), empty);

    //Assert
    verify(service).getAllDemandPlans();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getDemandPlansByProductIdShouldReturnList(boolean empty)
    throws Exception {
    //Arrange
    when(service.getDemandPlansByProductId(10L))
      .thenReturn(empty ? List.of() : List.of(response));

    //Act + Assert
    assertList(get("/demand-plans/product/10"), empty);

    //Assert
    verify(service).getDemandPlansByProductId(10L);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getDemandPlansByRequiredDateShouldReturnList(boolean empty)
    throws Exception {
    //Arrange
    when(service.getDemandPlansByRequiredDate(date))
      .thenReturn(empty ? List.of() : List.of(response));

    //Act + Assert
    assertList(get("/demand-plans/date/{date}", date), empty);

    //Assert
    verify(service).getDemandPlansByRequiredDate(date);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getDemandPlansByDateRangeShouldReturnList(boolean empty)
    throws Exception {
    //Arrange
    LocalDate to = date.plusDays(2);

    when(service.getDemandPlansByDateRange(date, to))
      .thenReturn(empty ? List.of() : List.of(response));

    //Act + Assert
    assertList(get("/demand-plans/date-range")
      .param("from", date.toString())
      .param("to", to.toString()), empty);

    //Assert
    verify(service).getDemandPlansByDateRange(date, to);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getFutureDemandPlansShouldReturnList(boolean empty)
    throws Exception {
    //Arrange
    when(service.getFutureDemandPlans())
      .thenReturn(empty ? List.of() : List.of(response));

    //Act + Assert
    assertList(get("/demand-plans/future"), empty);

    //Assert
    verify(service).getFutureDemandPlans();
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "/demand-plans/not-a-number",
    "/demand-plans/date/not-a-date",
    "/demand-plans/date-range",
    "/demand-plans/date-range?from=2026-10-20",
    "/demand-plans/date-range?to=2026-10-20",
    "/demand-plans/date-range?from=bad&to=2026-10-20",
    "/demand-plans/date-range?from=2026-10-20&to=bad"
  })
  public void getShouldRejectInvalidParameters(String url) throws Exception {
    //Act + Assert
    mockMvc.perform(get(url))
      .andExpect(status().isBadRequest());

    //Assert
    verifyNoInteractions(service);
  }

  @Test
  public void updateDemandPlanShouldReturnUpdatedDto() throws Exception {
    //Arrange
    DemandPlanRequestDto update =
      new DemandPlanRequestDto(10L, date, 200);

    when(service.updateDemandPlan(20L, update))
      .thenReturn(new DemandPlanResponseDto(20L, 10L, date, 200));

    //Act + Assert
    mockMvc.perform(put("/demand-plans/20")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json.replace("100", "200")))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(20))
      .andExpect(jsonPath("$.productId").value(10))
      .andExpect(jsonPath("$.requiredDate").value(date.toString()))
      .andExpect(jsonPath("$.requiredQuantity").value(200));

    //Assert
    verify(service).updateDemandPlan(20L, update);
  }

  @ParameterizedTest
  @MethodSource("updateExceptions")
  public void updateDemandPlanShouldReturnNotFound(RuntimeException exception)
    throws Exception {
    //Arrange
    when(service.updateDemandPlan(20L, request)).thenThrow(exception);

    //Act + Assert
    mockMvc.perform(put("/demand-plans/20")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
      .andExpect(status().isNotFound());
  }

  @ParameterizedTest
  @MethodSource("invalidRequests")
  public void updateDemandPlanShouldRejectInvalidData(String invalidJson)
    throws Exception {
    //Act + Assert
    mockMvc.perform(put("/demand-plans/20")
        .contentType(MediaType.APPLICATION_JSON)
        .content(invalidJson))
      .andExpect(status().isBadRequest());

    //Assert
    verifyNoInteractions(service);
  }

  @Test
  public void deleteDemandPlanShouldReturnNoContent() throws Exception {
    //Act + Assert
    mockMvc.perform(delete("/demand-plans/20"))
      .andExpect(status().isNoContent())
      .andExpect(content().string(""));

    //Assert
    verify(service).deleteDemandPlan(20L);
  }

  @Test
  public void deleteDemandPlanShouldReturnNotFound() throws Exception {
    //Arrange
    doThrow(new DemandPlanIsNotFoundException("Plan not found"))
      .when(service).deleteDemandPlan(20L);

    //Act + Assert
    mockMvc.perform(delete("/demand-plans/20"))
      .andExpect(status().isNotFound());
  }

  private void assertList(
    MockHttpServletRequestBuilder request,
    boolean empty
  ) throws Exception {
    ResultActions result = mockMvc.perform(request)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(empty ? 0 : 1));

    if (!empty) {
      result.andExpect(jsonPath("$[0].id").value(20))
        .andExpect(jsonPath("$[0].productId").value(10))
        .andExpect(jsonPath("$[0].requiredQuantity").value(100));
    }
  }

  private static Stream<RuntimeException> updateExceptions() {
    return Stream.of(
      new DemandPlanIsNotFoundException("Plan not found"),
      new ProductIsNotFoundException("Product not found")
    );
  }

  private static Stream<String> invalidRequests() {
    String future = LocalDate.now().plusWeeks(1).toString();
    String past = LocalDate.now().minusDays(1).toString();

    return Stream.of(
      """
      {"productId":null,"requiredDate":"%s","requiredQuantity":100}
      """.formatted(future),
      """
      {"productId":10,"requiredDate":null,"requiredQuantity":100}
      """,
      """
      {"productId":10,"requiredDate":"%s","requiredQuantity":null}
      """.formatted(future),
      """
      {"productId":10,"requiredDate":"%s","requiredQuantity":0}
      """.formatted(future),
      """
      {"productId":10,"requiredDate":"%s","requiredQuantity":-1}
      """.formatted(future),
      """
      {"productId":10,"requiredDate":"%s","requiredQuantity":100}
      """.formatted(past)
    );
  }
}
