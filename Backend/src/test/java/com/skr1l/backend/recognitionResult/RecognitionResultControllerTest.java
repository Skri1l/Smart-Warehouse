package com.skr1l.backend.recognitionResult;

import com.skr1l.backend.exception.GlobalExceptionHandler;
import com.skr1l.backend.exception.imageEvent.ImageEventIsNotFoundException;
import com.skr1l.backend.exception.productExceptions.ProductIsNotFoundException;
import com.skr1l.backend.exception.recognitionResult.RecognitionResultIsNotFoundException;
import com.skr1l.backend.recognitionResult.controller.RecognitionResultController;
import com.skr1l.backend.recognitionResult.dto.*;
import com.skr1l.backend.recognitionResult.enums.RecognitionStatus;
import com.skr1l.backend.recognitionResult.service.RecognitionResultService;
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

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class RecognitionResultControllerTest {

  @Mock
  private RecognitionResultService service;

  private MockMvc mockMvc;
  private RecognitionResultRequestDto request;
  private RecognitionResultResponseDto response;

  private final String json = """
        {
            "imageEventId": 40,
            "productId": 10,
            "detectedQuantity": 14,
            "quantityDelta": -2,
            "confidence": 0.95
        }
        """;

  @BeforeEach
  public void setup() {
    request = new RecognitionResultRequestDto(40L, 10L, 14, -2, 0.95);
    response = new RecognitionResultResponseDto(
      50L, 40L, 10L, 14, -2, 0.95, RecognitionStatus.DETECTED
    );

    mockMvc = MockMvcBuilders
      .standaloneSetup(new RecognitionResultController(service))
      .setControllerAdvice(new GlobalExceptionHandler())
      .build();
  }

  @Test
  public void createRecognitionResultShouldReturnCreatedAndId() throws Exception {
    //Arrange
    when(service.createRecognitionResult(request)).thenReturn(50L);

    //Act + Assert
    mockMvc.perform(post("/recognition-results")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
      .andExpect(status().isCreated())
      .andExpect(content().string("50"));

    //Assert
    verify(service).createRecognitionResult(request);
  }

  @ParameterizedTest
  @MethodSource("creationExceptions")
  public void createRecognitionResultShouldReturnNotFound(
    RuntimeException exception
  ) throws Exception {
    //Arrange
    when(service.createRecognitionResult(request)).thenThrow(exception);

    //Act + Assert
    mockMvc.perform(post("/recognition-results")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
      .andExpect(status().isNotFound());
  }

  @ParameterizedTest
  @ValueSource(strings = {
    """
    {"imageEventId":null,"productId":10,"detectedQuantity":14,"quantityDelta":-2,"confidence":0.95}
    """,
    """
    {"imageEventId":40,"productId":null,"detectedQuantity":14,"quantityDelta":-2,"confidence":0.95}
    """,
    """
    {"imageEventId":40,"productId":10,"detectedQuantity":null,"quantityDelta":-2,"confidence":0.95}
    """,
    """
    {"imageEventId":40,"productId":10,"detectedQuantity":14,"quantityDelta":null,"confidence":0.95}
    """,
    """
    {"imageEventId":40,"productId":10,"detectedQuantity":14,"quantityDelta":-2,"confidence":null}
    """
  })
  public void createRecognitionResultShouldRejectInvalidData(String invalidJson)
    throws Exception {
    //Act + Assert
    mockMvc.perform(post("/recognition-results")
        .contentType(MediaType.APPLICATION_JSON)
        .content(invalidJson))
      .andExpect(status().isBadRequest());

    //Assert
    verifyNoInteractions(service);
  }

  @Test
  public void getRecognitionResultByIdShouldReturnDto() throws Exception {
    //Arrange
    when(service.getRecognitionResultById(50L)).thenReturn(response);

    //Act + Assert
    mockMvc.perform(get("/recognition-results/50"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(50))
      .andExpect(jsonPath("$.imageEventId").value(40))
      .andExpect(jsonPath("$.productId").value(10))
      .andExpect(jsonPath("$.detectedQuantity").value(14))
      .andExpect(jsonPath("$.quantityDelta").value(-2))
      .andExpect(jsonPath("$.confidence").value(0.95))
      .andExpect(jsonPath("$.status").value("DETECTED"));
  }

  @Test
  public void getRecognitionResultByIdShouldReturnNotFound() throws Exception {
    //Arrange
    when(service.getRecognitionResultById(50L))
      .thenThrow(new RecognitionResultIsNotFoundException("Result not found"));

    //Act + Assert
    mockMvc.perform(get("/recognition-results/50"))
      .andExpect(status().isNotFound());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getAllRecognitionResultsShouldReturnList(boolean empty)
    throws Exception {
    //Arrange
    when(service.getAllRecognitionResults())
      .thenReturn(empty ? List.of() : List.of(response));

    //Act + Assert
    assertList(get("/recognition-results"), empty);

    //Assert
    verify(service).getAllRecognitionResults();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getRecognitionResultsByImageEventIdShouldReturnList(boolean empty)
    throws Exception {
    //Arrange
    when(service.getRecognitionResultsByImageEventId(40L))
      .thenReturn(empty ? List.of() : List.of(response));

    //Act + Assert
    assertList(get("/recognition-results/image-event/40"), empty);

    //Assert
    verify(service).getRecognitionResultsByImageEventId(40L);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getRecognitionResultsByProductIdShouldReturnList(boolean empty)
    throws Exception {
    //Arrange
    when(service.getRecognitionResultsByProductId(10L))
      .thenReturn(empty ? List.of() : List.of(response));

    //Act + Assert
    assertList(get("/recognition-results/product/10"), empty);

    //Assert
    verify(service).getRecognitionResultsByProductId(10L);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getRecognitionResultsByStatusShouldReturnList(boolean empty)
    throws Exception {
    //Arrange
    when(service.getRecognitionResultsByStatus(RecognitionStatus.DETECTED))
      .thenReturn(empty ? List.of() : List.of(response));

    //Act + Assert
    assertList(get("/recognition-results/status/DETECTED"), empty);

    //Assert
    verify(service).getRecognitionResultsByStatus(RecognitionStatus.DETECTED);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getRecognitionResultsByMinConfidenceShouldReturnList(boolean empty)
    throws Exception {
    //Arrange
    when(service.getRecognitionResultsByMinConfidence(0.9))
      .thenReturn(empty ? List.of() : List.of(response));

    //Act + Assert
    assertList(get("/recognition-results/confidence")
      .param("min", "0.9"), empty);

    //Assert
    verify(service).getRecognitionResultsByMinConfidence(0.9);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "/recognition-results/not-a-number",
    "/recognition-results/status/UNKNOWN",
    "/recognition-results/confidence",
    "/recognition-results/confidence?min=bad"
  })
  public void getShouldRejectInvalidParameters(String url) throws Exception {
    //Act + Assert
    mockMvc.perform(get(url))
      .andExpect(status().isBadRequest());

    //Assert
    verifyNoInteractions(service);
  }

  @ParameterizedTest
  @ValueSource(strings = {"confirm", "reject", "failed"})
  public void statusEndpointsShouldReturnNoContent(String action)
    throws Exception {
    //Act + Assert
    mockMvc.perform(patch("/recognition-results/50/" + action))
      .andExpect(status().isNoContent())
      .andExpect(content().string(""));

    //Assert
    verifyStatusCall(action);
  }

  @ParameterizedTest
  @ValueSource(strings = {"confirm", "reject", "failed"})
  public void statusEndpointsShouldReturnNotFound(String action)
    throws Exception {
    //Arrange
    RecognitionResultIsNotFoundException exception =
      new RecognitionResultIsNotFoundException("Result not found");

    switch (action) {
      case "confirm" ->
        doThrow(exception).when(service).confirmRecognitionResult(50L);
      case "reject" ->
        doThrow(exception).when(service).rejectRecognitionResult(50L);
      case "failed" ->
        doThrow(exception).when(service).markRecognitionResultAsFailed(50L);
    }

    //Act + Assert
    mockMvc.perform(patch("/recognition-results/50/" + action))
      .andExpect(status().isNotFound());

    //Assert
    verifyStatusCall(action);
  }

  @Test
  public void deleteRecognitionResultShouldReturnNoContent() throws Exception {
    //Act + Assert
    mockMvc.perform(delete("/recognition-results/50"))
      .andExpect(status().isNoContent())
      .andExpect(content().string(""));

    //Assert
    verify(service).deleteRecognitionResult(50L);
  }

  @Test
  public void deleteRecognitionResultShouldReturnNotFound() throws Exception {
    //Arrange
    doThrow(new RecognitionResultIsNotFoundException("Result not found"))
      .when(service).deleteRecognitionResult(50L);

    //Act + Assert
    mockMvc.perform(delete("/recognition-results/50"))
      .andExpect(status().isNotFound());
  }

  private void verifyStatusCall(String action) {
    switch (action) {
      case "confirm" -> verify(service).confirmRecognitionResult(50L);
      case "reject" -> verify(service).rejectRecognitionResult(50L);
      case "failed" -> verify(service).markRecognitionResultAsFailed(50L);
      default -> throw new IllegalArgumentException(action);
    }
  }

  private void assertList(
    MockHttpServletRequestBuilder request,
    boolean empty
  ) throws Exception {
    ResultActions result = mockMvc.perform(request)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(empty ? 0 : 1));

    if (!empty) {
      result.andExpect(jsonPath("$[0].id").value(50))
        .andExpect(jsonPath("$[0].imageEventId").value(40))
        .andExpect(jsonPath("$[0].productId").value(10))
        .andExpect(jsonPath("$[0].status").value("DETECTED"));
    }
  }

  private static Stream<RuntimeException> creationExceptions() {
    return Stream.of(
      new ImageEventIsNotFoundException("Image event not found"),
      new ProductIsNotFoundException("Product not found")
    );
  }
}
