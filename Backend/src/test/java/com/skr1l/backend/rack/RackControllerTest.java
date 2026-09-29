package com.skr1l.backend.rack;

import com.skr1l.backend.rack.controller.RackController;
import com.skr1l.backend.rack.dto.RackRequestDto;
import com.skr1l.backend.rack.dto.RackResponseDto;
import com.skr1l.backend.rack.service.RackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RackControllerTest {

    @Mock
    private RackService rackService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RackController controller = new RackController(rackService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void getRackShouldReturnRack() throws Exception {
        // Arrange
        RackResponseDto response = new RackResponseDto(10L, "Rack A", 1L);

        when(rackService.getRackById(10L))
                .thenReturn(response);

        // Act + Assert
        mockMvc.perform(get("/racks/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.label").value("Rack A"))
                .andExpect(jsonPath("$.warehouseId").value(1));

        verify(rackService).getRackById(10L);
    }

    @Test
    void getAllRacksShouldReturnRacks() throws Exception {
        // Arrange
        when(rackService.getAllRacks())
                .thenReturn(List.of(new RackResponseDto(10L, "Rack A", 1L)));

        // Act
        // Assert
        mockMvc.perform(get("/racks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label")
                        .value("Rack A"));
    }

    @Test
    void createRackShouldReturnCreatedAndId() throws Exception {
        // Arrange
        when(rackService.createRack(any(RackRequestDto.class), eq(1L))).thenReturn(10L);

        String json = """
                {
                    "label": "Rack A",
                    "warehouseId": 1
                }
                """;

        // Act
        // Assert
        mockMvc.perform(post("/racks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().string("10"));
    }

    @Test
    void updateRackShouldReturnUpdatedRack() throws Exception {
        // Arrange
        when(rackService.updateRack(any(RackRequestDto.class), eq(10L), eq(1L)))
                .thenReturn(new RackResponseDto(10L, "Rack B", 1L));

        String json = """
                {
                    "label": "Rack B",
                    "warehouseId": 1
                }
                """;

        // Act
        // Assert
        mockMvc.perform(put("/racks/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label")
                        .value("Rack B"));
    }

    @Test
    void deleteRackShouldReturnNoContent() throws Exception {
        //Arrange
        mockMvc.perform(delete("/racks/10"))
                .andExpect(status().isNoContent());

        //Act
        //Assert
        verify(rackService).deleteRackById(10L);
    }
}