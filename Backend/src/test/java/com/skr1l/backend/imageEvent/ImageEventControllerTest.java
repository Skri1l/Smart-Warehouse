package com.skr1l.backend.imageEvent;

import com.skr1l.backend.exception.GlobalExceptionHandler;
import com.skr1l.backend.exception.deviceExceptions.DeviceIsNotFoundException;
import com.skr1l.backend.exception.imageEvent.ImageEventIsNotFoundException;
import com.skr1l.backend.imageEvent.controller.ImageEventController;
import com.skr1l.backend.imageEvent.dto.*;
import com.skr1l.backend.imageEvent.enums.ImageProcessingStatus;
import com.skr1l.backend.imageEvent.service.ImageEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ImageEventControllerTest {

  @Mock
  private ImageEventService service;

  private MockMvc mockMvc;
  private ImageEventRequestDto request;
  private ImageEventResponseDto response;
  private String json;

  private final LocalDateTime time =
    LocalDateTime.of(2026, 10, 15, 12, 0);

  @BeforeEach
  public void setup() {
    request = new ImageEventRequestDto(20L, "/images/test.jpg", time);
    response = new ImageEventResponseDto(
      40L, 20L, 30L, "/images/test.jpg",
      time, time.plusSeconds(1), ImageProcessingStatus.RECEIVED
    );

    json = """
            {
                "deviceId": 20,
                "imagePath": "/images/test.jpg",
                "capturedAt": "%s"
            }
            """.formatted(time);

    mockMvc = MockMvcBuilders
      .standaloneSetup(new ImageEventController(service))
      .setControllerAdvice(new GlobalExceptionHandler())
      .build();
  }

  @Test
  public void createImageEventShouldReturnCreatedAndId() throws Exception {
    //Arrange
    when(service.createImageEvent(request)).thenReturn(40L);

    //Act + Assert
    mockMvc.perform(post("/image-events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
      .andExpect(status().isCreated())
      .andExpect(content().string("40"));

    //Assert
    verify(service).createImageEvent(request);
  }

  @Test
  public void createImageEventShouldReturnNotFoundWhenDeviceDoesNotExist()
    throws Exception {
    //Arrange
    when(service.createImageEvent(request))
      .thenThrow(new DeviceIsNotFoundException("Device not found"));

    //Act + Assert
    mockMvc.perform(post("/image-events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
      .andExpect(status().isNotFound());
  }

  @ParameterizedTest
  @ValueSource(strings = {
    """
    {"deviceId":null,"imagePath":"/image.jpg","capturedAt":"2026-10-15T12:00:00"}
    """,
    """
    {"deviceId":20,"imagePath":null,"capturedAt":"2026-10-15T12:00:00"}
    """,
    """
    {"deviceId":20,"imagePath":"","capturedAt":"2026-10-15T12:00:00"}
    """,
    """
    {"deviceId":20,"imagePath":"   ","capturedAt":"2026-10-15T12:00:00"}
    """,
    """
    {"deviceId":20,"imagePath":"/image.jpg","capturedAt":null}
    """
  })
  public void createImageEventShouldRejectInvalidData(String invalidJson)
    throws Exception {
    //Act + Assert
    mockMvc.perform(post("/image-events")
        .contentType(MediaType.APPLICATION_JSON)
        .content(invalidJson))
      .andExpect(status().isBadRequest());

    //Assert
    verifyNoInteractions(service);
  }

  @Test
  public void getImageEventByIdShouldReturnDto() throws Exception {
    //Arrange
    when(service.getImageEventById(40L)).thenReturn(response);

    //Act + Assert
    mockMvc.perform(get("/image-events/40"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(40))
      .andExpect(jsonPath("$.deviceId").value(20))
      .andExpect(jsonPath("$.shelfId").value(30))
      .andExpect(jsonPath("$.imagePath").value("/images/test.jpg"))
      .andExpect(jsonPath("$.processingStatus").value("RECEIVED"));
  }

  @Test
  public void getImageEventByIdShouldReturnNotFound() throws Exception {
    //Arrange
    when(service.getImageEventById(40L))
      .thenThrow(new ImageEventIsNotFoundException("Event not found"));

    //Act + Assert
    mockMvc.perform(get("/image-events/40"))
      .andExpect(status().isNotFound());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getAllImageEventsShouldReturnList(boolean empty) throws Exception {
    //Arrange
    when(service.getAllImageEvents())
      .thenReturn(empty ? List.of() : List.of(response));

    //Act + Assert
    assertList(get("/image-events"), empty);

    //Assert
    verify(service).getAllImageEvents();
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getImageEventsByDeviceIdShouldReturnList(boolean empty)
    throws Exception {
    //Arrange
    when(service.getImageEventsByDeviceId(20L))
      .thenReturn(empty ? List.of() : List.of(response));

    //Act + Assert
    assertList(get("/image-events/device/20"), empty);

    //Assert
    verify(service).getImageEventsByDeviceId(20L);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getImageEventsByShelfIdShouldReturnList(boolean empty)
    throws Exception {
    //Arrange
    when(service.getImageEventsByShelfId(30L))
      .thenReturn(empty ? List.of() : List.of(response));

    //Act + Assert
    assertList(get("/image-events/shelf/30"), empty);

    //Assert
    verify(service).getImageEventsByShelfId(30L);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getImageEventsByStatusShouldReturnList(boolean empty)
    throws Exception {
    //Arrange
    when(service.getImageEventsByStatus(ImageProcessingStatus.RECEIVED))
      .thenReturn(empty ? List.of() : List.of(response));

    //Act + Assert
    assertList(get("/image-events/status/RECEIVED"), empty);

    //Assert
    verify(service).getImageEventsByStatus(ImageProcessingStatus.RECEIVED);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  public void getImageEventsByDateRangeShouldReturnList(boolean empty)
    throws Exception {
    //Arrange
    LocalDateTime to = time.plusHours(1);

    when(service.getImageEventsByDateRange(time, to))
      .thenReturn(empty ? List.of() : List.of(response));

    //Act + Assert
    assertList(get("/image-events/date-range")
      .param("from", time.toString())
      .param("to", to.toString()), empty);

    //Assert
    verify(service).getImageEventsByDateRange(time, to);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "/image-events/not-a-number",
    "/image-events/status/UNKNOWN",
    "/image-events/date-range",
    "/image-events/date-range?from=2026-10-15T12:00:00",
    "/image-events/date-range?to=2026-10-15T12:00:00",
    "/image-events/date-range?from=bad&to=2026-10-15T12:00:00",
    "/image-events/date-range?from=2026-10-15T12:00:00&to=bad"
  })
  public void getShouldRejectInvalidParameters(String url) throws Exception {
    //Act + Assert
    mockMvc.perform(get(url))
      .andExpect(status().isBadRequest());

    //Assert
    verifyNoInteractions(service);
  }

  @ParameterizedTest
  @ValueSource(strings = {"processing", "processed", "failed"})
  public void statusEndpointsShouldReturnNoContent(String action)
    throws Exception {
    //Act + Assert
    mockMvc.perform(patch("/image-events/40/" + action))
      .andExpect(status().isNoContent())
      .andExpect(content().string(""));

    //Assert
    verifyStatusCall(action);
  }

  @ParameterizedTest
  @ValueSource(strings = {"processing", "processed", "failed"})
  public void statusEndpointsShouldReturnNotFound(String action)
    throws Exception {
    //Arrange
    ImageEventIsNotFoundException exception =
      new ImageEventIsNotFoundException("Event not found");

    switch (action) {
      case "processing" ->
        doThrow(exception).when(service).markAsProcessing(40L);
      case "processed" ->
        doThrow(exception).when(service).markAsProcessed(40L);
      case "failed" ->
        doThrow(exception).when(service).markAsFailed(40L);
    }

    //Act + Assert
    mockMvc.perform(patch("/image-events/40/" + action))
      .andExpect(status().isNotFound());

    //Assert
    verifyStatusCall(action);
  }

  @Test
  public void deleteImageEventShouldReturnNoContent() throws Exception {
    //Act + Assert
    mockMvc.perform(delete("/image-events/40"))
      .andExpect(status().isNoContent())
      .andExpect(content().string(""));

    //Assert
    verify(service).deleteImageEvent(40L);
  }

  @Test
  public void deleteImageEventShouldReturnNotFound() throws Exception {
    //Arrange
    doThrow(new ImageEventIsNotFoundException("Event not found"))
      .when(service).deleteImageEvent(40L);

    //Act + Assert
    mockMvc.perform(delete("/image-events/40"))
      .andExpect(status().isNotFound());
  }

  private void verifyStatusCall(String action) {
    switch (action) {
      case "processing" -> verify(service).markAsProcessing(40L);
      case "processed" -> verify(service).markAsProcessed(40L);
      case "failed" -> verify(service).markAsFailed(40L);
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
      result.andExpect(jsonPath("$[0].id").value(40))
        .andExpect(jsonPath("$[0].deviceId").value(20))
        .andExpect(jsonPath("$[0].shelfId").value(30))
        .andExpect(jsonPath("$[0].processingStatus").value("RECEIVED"));
    }
  }
}
