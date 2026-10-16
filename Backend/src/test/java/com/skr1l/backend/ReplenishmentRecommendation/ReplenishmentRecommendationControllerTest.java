package com.skr1l.backend.ReplenishmentRecommendation;

import com.skr1l.backend.exception.GlobalExceptionHandler;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import com.skr1l.backend.exception.ReplenishmentRecommendationExceptions.ReplenishmentRecommendationIsNotFoundException;
import com.skr1l.backend.replenishmentRecommendation.controller.ReplenishmentRecommendationController;
import com.skr1l.backend.replenishmentRecommendation.dto.ReplenishmentRecommendationRequestDto;
import com.skr1l.backend.replenishmentRecommendation.dto.ReplenishmentRecommendationResponseDto;
import com.skr1l.backend.replenishmentRecommendation.enums.RecommendationStatus;
import com.skr1l.backend.replenishmentRecommendation.service.ReplenishmentRecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.skr1l.backend.exception.productExceptions.ProductIsNotFoundException;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class ReplenishmentRecommendationControllerTest {

  @Mock
  private ReplenishmentRecommendationService replenishmentRecommendationService;

  private MockMvc mockMvc;

  private ReplenishmentRecommendationResponseDto responseDto;

  @BeforeEach
  public void setup() {
    mockMvc = MockMvcBuilders
      .standaloneSetup(new ReplenishmentRecommendationController(replenishmentRecommendationService))
      .setControllerAdvice(new GlobalExceptionHandler())
      .build();

    responseDto = new ReplenishmentRecommendationResponseDto(
      10L,
      20L,
      100,
      LocalDate.now().plusWeeks(1),
      "reason",
      RecommendationStatus.NEW,
      LocalDateTime.now()
    );
  }

  @Test
  public void getRecommendationByIdShouldReturnReplenishmentRecommendationResponseDto() throws Exception {
    //Arrange
     when(replenishmentRecommendationService.getRecommendationById(10L))
       .thenReturn(responseDto);

     //Act + Assert
     mockMvc.perform(get("/replenishment-recommendations/10"))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$.id").value(10))
       .andExpect(jsonPath("$.productId").value(20))
       .andExpect(jsonPath("$.recommendedQuantity").value(100))
       .andExpect(jsonPath("$.status").value(RecommendationStatus.NEW.toString()))
       .andExpect(jsonPath("$.reason").value("reason"));
  }

  @Test
  public void getRecommendationByIdShouldThrowExceptionIfRecommendationIsInvalid() throws Exception {
    //Arrange
    when(replenishmentRecommendationService.getRecommendationById(10L))
      .thenThrow(ReplenishmentRecommendationIsNotFoundException.class);

    //Act + Assert
    mockMvc.perform(get("/replenishment-recommendations/10"))
      .andExpect(status().isNotFound());
  }

  @Test
  public void getAllRecommendationsShouldReturnListOfReplenishmentRecommendationResponseDto() throws Exception {
    //Arrange
    when(replenishmentRecommendationService.getAllRecommendations())
      .thenReturn(List.of(responseDto));

    //Act + Assert
    mockMvc.perform(get("/replenishment-recommendations"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].id").value(10))
      .andExpect(jsonPath("$[0].productId").value(20))
      .andExpect(jsonPath("$[0].recommendedQuantity").value(100))
      .andExpect(jsonPath("$[0].status").value(RecommendationStatus.NEW.toString()));
  }

  @Test
  public void getAllRecommendationsShouldReturnNothingIfRecommendationHasNoObjects() throws Exception {
    //Arrange
    when(replenishmentRecommendationService.getAllRecommendations())
      .thenReturn(List.of());

    //Act + Assert
    mockMvc.perform(get("/replenishment-recommendations"))
      .andExpect(status().isOk())
      .andExpect(content().json("[]"));
  }

  @Test
  public void createRecommendationShouldReturnCreatedAndId() throws Exception {
    //Arrange
    ReplenishmentRecommendationRequestDto dto = new ReplenishmentRecommendationRequestDto(
      10L, 100, LocalDate.now().plusWeeks(1), "reason");

    when(replenishmentRecommendationService.createRecommendation(dto))
      .thenReturn(30L);

    //Act
    mockMvc.perform(post("/replenishment-recommendations")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""
    {
        "productId": 10,
        "recommendedQuantity": 100,
        "requiredBy": "%s",
        "reason": "reason"
    }
    """
          .formatted(dto.requiredBy())))
      .andExpect(status().isCreated())
      .andExpect(content().string("30"));

    //Assert
    verify(replenishmentRecommendationService).createRecommendation(dto);
  }

  @Test
  public void createRecommendationShouldReturnNotFoundWhenProductDoesNotExist()
    throws Exception {
    //Arrange
    ReplenishmentRecommendationRequestDto dto = validRequest();

    when(replenishmentRecommendationService.createRecommendation(dto))
      .thenThrow(new ProductIsNotFoundException("Product not found"));

    //Act + Assert
    mockMvc.perform(post("/replenishment-recommendations")
        .contentType(MediaType.APPLICATION_JSON)
        .content(toJson(dto)))
      .andExpect(status().isNotFound());

    //Assert
    verify(replenishmentRecommendationService).createRecommendation(dto);
  }

  @Test
  public void createRecommendationShouldReturnBadRequestWhenDataIsInvalid()
    throws Exception {
    //Arrange
    List<String> requests = invalidRequests();

    //Act + Assert
    for (String json : requests) {
      mockMvc.perform(post("/replenishment-recommendations")
          .contentType(MediaType.APPLICATION_JSON)
          .content(json))
        .andExpect(status().isBadRequest());
    }

    //Assert
    verifyNoInteractions(replenishmentRecommendationService);
  }

  @Test
  public void getRecommendationsByProductIdShouldReturnMatchingRecommendations()
    throws Exception {
    //Arrange
    when(replenishmentRecommendationService.getRecommendationsByProductId(20L))
      .thenReturn(List.of(responseDto));

    //Act + Assert
    mockMvc.perform(get("/replenishment-recommendations/product/20"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].id").value(10))
      .andExpect(jsonPath("$[0].productId").value(20));

    //Assert
    verify(replenishmentRecommendationService).getRecommendationsByProductId(20L);
  }

  @Test
  public void getRecommendationsByProductIdShouldReturnEmptyListWhenNoRecommendationsMatch()
    throws Exception {
    //Arrange
    when(replenishmentRecommendationService.getRecommendationsByProductId(20L))
      .thenReturn(List.of());

    //Act + Assert
    mockMvc.perform(get("/replenishment-recommendations/product/20"))
      .andExpect(status().isOk())
      .andExpect(content().json("[]"));
  }

  @Test
  public void getRecommendationsByStatusShouldReturnMatchingRecommendations()
    throws Exception {
    //Arrange
    when(replenishmentRecommendationService
      .getRecommendationsByStatus(RecommendationStatus.NEW))
      .thenReturn(List.of(responseDto));

    //Act + Assert
    mockMvc.perform(get("/replenishment-recommendations/status/NEW"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].id").value(10))
      .andExpect(jsonPath("$[0].status").value("NEW"));

    //Assert
    verify(replenishmentRecommendationService)
      .getRecommendationsByStatus(RecommendationStatus.NEW);
  }

  @Test
  public void getRecommendationsByStatusShouldReturnEmptyListWhenNoRecommendationsMatch()
    throws Exception {
    //Arrange
    when(replenishmentRecommendationService
      .getRecommendationsByStatus(RecommendationStatus.APPROVED))
      .thenReturn(List.of());

    //Act + Assert
    mockMvc.perform(get("/replenishment-recommendations/status/APPROVED"))
      .andExpect(status().isOk())
      .andExpect(content().json("[]"));
  }

  @Test
  public void getRecommendationsByStatusShouldReturnBadRequestWhenStatusIsInvalid()
    throws Exception {
    //Act + Assert
    mockMvc.perform(get("/replenishment-recommendations/status/UNKNOWN"))
      .andExpect(status().isBadRequest());

    //Assert
    verifyNoInteractions(replenishmentRecommendationService);
  }

  @Test
  public void getRecommendationsByRequiredByShouldReturnMatchingRecommendations()
    throws Exception {
    //Arrange
    LocalDate date = responseDto.requiredBy();

    when(replenishmentRecommendationService.getRecommendationsByRequiredBy(date))
      .thenReturn(List.of(responseDto));

    //Act + Assert
    mockMvc.perform(get("/replenishment-recommendations/date/{requiredBy}", date))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].id").value(10))
      .andExpect(jsonPath("$[0].requiredBy").value(date.toString()));

    //Assert
    verify(replenishmentRecommendationService).getRecommendationsByRequiredBy(date);
  }

  @Test
  public void getRecommendationsByRequiredByShouldReturnEmptyListWhenNoRecommendationsMatch()
    throws Exception {
    //Arrange
    LocalDate date = responseDto.requiredBy();

    when(replenishmentRecommendationService.getRecommendationsByRequiredBy(date))
      .thenReturn(List.of());

    //Act + Assert
    mockMvc.perform(get("/replenishment-recommendations/date/{requiredBy}", date))
      .andExpect(status().isOk())
      .andExpect(content().json("[]"));
  }

  @Test
  public void getRecommendationsByRequiredByShouldReturnBadRequestWhenDateIsInvalid()
    throws Exception {
    //Act + Assert
    mockMvc.perform(get("/replenishment-recommendations/date/not-a-date"))
      .andExpect(status().isBadRequest());

    //Assert
    verifyNoInteractions(replenishmentRecommendationService);
  }

  @Test
  public void getRecommendationsByDateRangeShouldReturnMatchingRecommendations()
    throws Exception {
    //Arrange
    LocalDate from = responseDto.requiredBy().minusDays(1);
    LocalDate to = responseDto.requiredBy().plusDays(1);

    when(replenishmentRecommendationService.getRecommendationsByDateRange(from, to))
      .thenReturn(List.of(responseDto));

    //Act + Assert
    mockMvc.perform(get("/replenishment-recommendations/date-range")
        .param("from", from.toString())
        .param("to", to.toString()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].id").value(10))
      .andExpect(jsonPath("$[0].requiredBy")
        .value(responseDto.requiredBy().toString()));

    //Assert
    verify(replenishmentRecommendationService)
      .getRecommendationsByDateRange(from, to);
  }

  @Test
  public void getRecommendationsByDateRangeShouldReturnEmptyListWhenNoRecommendationsMatch()
    throws Exception {
    //Arrange
    LocalDate from = LocalDate.of(2026, 1, 1);
    LocalDate to = LocalDate.of(2026, 1, 31);

    when(replenishmentRecommendationService.getRecommendationsByDateRange(from, to))
      .thenReturn(List.of());

    //Act + Assert
    mockMvc.perform(get("/replenishment-recommendations/date-range")
        .param("from", from.toString())
        .param("to", to.toString()))
      .andExpect(status().isOk())
      .andExpect(content().json("[]"));
  }

  @Test
  public void getRecommendationsByDateRangeShouldReturnBadRequestWhenParameterIsMissing()
    throws Exception {
    //Arrange
    String date = "2026-10-20";

    //Act + Assert
    mockMvc.perform(get("/replenishment-recommendations/date-range")
        .param("from", date))
      .andExpect(status().isBadRequest());

    mockMvc.perform(get("/replenishment-recommendations/date-range")
        .param("to", date))
      .andExpect(status().isBadRequest());

    //Assert
    verifyNoInteractions(replenishmentRecommendationService);
  }

  @Test
  public void getRecommendationsByDateRangeShouldReturnBadRequestWhenDateIsInvalid()
    throws Exception {
    //Act + Assert
    mockMvc.perform(get("/replenishment-recommendations/date-range")
        .param("from", "not-a-date")
        .param("to", "2026-10-20"))
      .andExpect(status().isBadRequest());

    mockMvc.perform(get("/replenishment-recommendations/date-range")
        .param("from", "2026-10-10")
        .param("to", "not-a-date"))
      .andExpect(status().isBadRequest());

    //Assert
    verifyNoInteractions(replenishmentRecommendationService);
  }

  @Test
  public void updateRecommendationShouldReturnUpdatedRecommendation()
    throws Exception {
    //Arrange
    ReplenishmentRecommendationRequestDto dto =
      new ReplenishmentRecommendationRequestDto(
        20L, 200, responseDto.requiredBy().plusDays(1), "updated reason"
      );

    ReplenishmentRecommendationResponseDto updated =
      new ReplenishmentRecommendationResponseDto(
        10L,
        dto.productId(),
        dto.recommendedQuantity(),
        dto.requiredBy(),
        dto.reason(),
        RecommendationStatus.NEW,
        responseDto.createdAt()
      );

    when(replenishmentRecommendationService.updateRecommendation(10L, dto))
      .thenReturn(updated);

    //Act + Assert
    mockMvc.perform(put("/replenishment-recommendations/10")
        .contentType(MediaType.APPLICATION_JSON)
        .content(toJson(dto)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(10))
      .andExpect(jsonPath("$.productId").value(20))
      .andExpect(jsonPath("$.recommendedQuantity").value(200))
      .andExpect(jsonPath("$.requiredBy").value(dto.requiredBy().toString()))
      .andExpect(jsonPath("$.reason").value("updated reason"))
      .andExpect(jsonPath("$.status").value("NEW"));

    //Assert
    verify(replenishmentRecommendationService).updateRecommendation(10L, dto);
  }

  @Test
  public void updateRecommendationShouldReturnNotFoundWhenRecommendationDoesNotExist()
    throws Exception {
    //Arrange
    ReplenishmentRecommendationRequestDto dto = validRequest();

    when(replenishmentRecommendationService.updateRecommendation(10L, dto))
      .thenThrow(new ReplenishmentRecommendationIsNotFoundException(
        "Recommendation not found"));

    //Act + Assert
    mockMvc.perform(put("/replenishment-recommendations/10")
        .contentType(MediaType.APPLICATION_JSON)
        .content(toJson(dto)))
      .andExpect(status().isNotFound());

    //Assert
    verify(replenishmentRecommendationService).updateRecommendation(10L, dto);
  }

  @Test
  public void updateRecommendationShouldReturnNotFoundWhenProductDoesNotExist()
    throws Exception {
    //Arrange
    ReplenishmentRecommendationRequestDto dto = validRequest();

    when(replenishmentRecommendationService.updateRecommendation(10L, dto))
      .thenThrow(new ProductIsNotFoundException("Product not found"));

    //Act + Assert
    mockMvc.perform(put("/replenishment-recommendations/10")
        .contentType(MediaType.APPLICATION_JSON)
        .content(toJson(dto)))
      .andExpect(status().isNotFound());

    //Assert
    verify(replenishmentRecommendationService).updateRecommendation(10L, dto);
  }

  @Test
  public void updateRecommendationShouldReturnBadRequestWhenDataIsInvalid()
    throws Exception {
    //Arrange
    List<String> requests = invalidRequests();

    //Act + Assert
    for (String json : requests) {
      mockMvc.perform(put("/replenishment-recommendations/10")
          .contentType(MediaType.APPLICATION_JSON)
          .content(json))
        .andExpect(status().isBadRequest());
    }

    //Assert
    verifyNoInteractions(replenishmentRecommendationService);
  }

  @Test
  public void approveRecommendationShouldReturnNoContent() throws Exception {
    //Arrange

    //Act + Assert
    mockMvc.perform(patch("/replenishment-recommendations/10/approve"))
      .andExpect(status().isNoContent())
      .andExpect(content().string(""));

    //Assert
    verify(replenishmentRecommendationService).approveRecommendation(10L);
  }

  @Test
  public void approveRecommendationShouldReturnNotFoundWhenRecommendationDoesNotExist()
    throws Exception {
    //Arrange
    doThrow(new ReplenishmentRecommendationIsNotFoundException(
      "Recommendation not found"))
      .when(replenishmentRecommendationService).approveRecommendation(10L);

    //Act + Assert
    mockMvc.perform(patch("/replenishment-recommendations/10/approve"))
      .andExpect(status().isNotFound());

    //Assert
    verify(replenishmentRecommendationService).approveRecommendation(10L);
  }

  @Test
  public void rejectRecommendationShouldReturnNoContent() throws Exception {
    //Arrange

    //Act + Assert
    mockMvc.perform(patch("/replenishment-recommendations/10/reject"))
      .andExpect(status().isNoContent())
      .andExpect(content().string(""));

    //Assert
    verify(replenishmentRecommendationService).rejectRecommendation(10L);
  }

  @Test
  public void rejectRecommendationShouldReturnNotFoundWhenRecommendationDoesNotExist()
    throws Exception {
    //Arrange
    doThrow(new ReplenishmentRecommendationIsNotFoundException(
      "Recommendation not found"))
      .when(replenishmentRecommendationService).rejectRecommendation(10L);

    //Act + Assert
    mockMvc.perform(patch("/replenishment-recommendations/10/reject"))
      .andExpect(status().isNotFound());

    //Assert
    verify(replenishmentRecommendationService).rejectRecommendation(10L);
  }

  @Test
  public void markRecommendationAsOrderedShouldReturnNoContent() throws Exception {
    //Arrange

    //Act + Assert
    mockMvc.perform(patch("/replenishment-recommendations/10/ordered"))
      .andExpect(status().isNoContent())
      .andExpect(content().string(""));

    //Assert
    verify(replenishmentRecommendationService).markRecommendationAsOrdered(10L);
  }

  @Test
  public void markRecommendationAsOrderedShouldReturnNotFoundWhenRecommendationDoesNotExist()
    throws Exception {
    //Arrange
    doThrow(new ReplenishmentRecommendationIsNotFoundException(
      "Recommendation not found"))
      .when(replenishmentRecommendationService).markRecommendationAsOrdered(10L);

    //Act + Assert
    mockMvc.perform(patch("/replenishment-recommendations/10/ordered"))
      .andExpect(status().isNotFound());

    //Assert
    verify(replenishmentRecommendationService).markRecommendationAsOrdered(10L);
  }

  @Test
  public void completeRecommendationShouldReturnNoContent() throws Exception {
    //Arrange

    //Act + Assert
    mockMvc.perform(patch("/replenishment-recommendations/10/complete"))
      .andExpect(status().isNoContent())
      .andExpect(content().string(""));

    //Assert
    verify(replenishmentRecommendationService).completeRecommendation(10L);
  }

  @Test
  public void completeRecommendationShouldReturnNotFoundWhenRecommendationDoesNotExist()
    throws Exception {
    //Arrange
    doThrow(new ReplenishmentRecommendationIsNotFoundException(
      "Recommendation not found"))
      .when(replenishmentRecommendationService).completeRecommendation(10L);

    //Act + Assert
    mockMvc.perform(patch("/replenishment-recommendations/10/complete"))
      .andExpect(status().isNotFound());

    //Assert
    verify(replenishmentRecommendationService).completeRecommendation(10L);
  }

  @Test
  public void deleteRecommendationShouldReturnNoContent() throws Exception {
    //Arrange

    //Act + Assert
    mockMvc.perform(delete("/replenishment-recommendations/10"))
      .andExpect(status().isNoContent())
      .andExpect(content().string(""));

    //Assert
    verify(replenishmentRecommendationService).deleteRecommendation(10L);
  }

  @Test
  public void deleteRecommendationShouldReturnNotFoundWhenRecommendationDoesNotExist()
    throws Exception {
    //Arrange
    doThrow(new ReplenishmentRecommendationIsNotFoundException(
      "Recommendation not found"))
      .when(replenishmentRecommendationService).deleteRecommendation(10L);

    //Act + Assert
    mockMvc.perform(delete("/replenishment-recommendations/10"))
      .andExpect(status().isNotFound());

    //Assert
    verify(replenishmentRecommendationService).deleteRecommendation(10L);
  }

  private ReplenishmentRecommendationRequestDto validRequest() {
    return new ReplenishmentRecommendationRequestDto(
      20L, 100, responseDto.requiredBy(), "reason"
    );
  }

  private String toJson(ReplenishmentRecommendationRequestDto dto) {
    return """
        {
            "productId": %s,
            "recommendedQuantity": %s,
            "requiredBy": "%s",
            "reason": "%s"
        }
        """.formatted(
      dto.productId(),
      dto.recommendedQuantity(),
      dto.requiredBy(),
      dto.reason()
    );
  }

  private List<String> invalidRequests() {
    String template = """
        {
            "productId": %s,
            "recommendedQuantity": %s,
            "requiredBy": %s,
            "reason": %s
        }
        """;

    String futureDate = "\"" + responseDto.requiredBy() + "\"";
    String pastDate = "\"" + LocalDate.now().minusDays(1) + "\"";

    return List.of(
      template.formatted("null", "100", futureDate, "\"reason\""),
      template.formatted("20", "null", futureDate, "\"reason\""),
      template.formatted("20", "0", futureDate, "\"reason\""),
      template.formatted("20", "-1", futureDate, "\"reason\""),
      template.formatted("20", "100", "null", "\"reason\""),
      template.formatted("20", "100", pastDate, "\"reason\""),
      template.formatted("20", "100", futureDate, "null"),
      template.formatted("20", "100", futureDate, "\"\""),
      template.formatted("20", "100", futureDate, "\"   \"")
    );
  }
}
