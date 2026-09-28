package com.skr1l.backend.warehouse;

import com.skr1l.backend.warehouse.controller.WarehouseController;
import com.skr1l.backend.warehouse.dto.WarehouseRequestDTO;
import com.skr1l.backend.warehouse.dto.WarehouseResponseDTO;
import com.skr1l.backend.warehouse.service.WarehouseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WarehouseControllerTest {

    @Mock
    private WarehouseService warehouseService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        WarehouseController controller =
                new WarehouseController(warehouseService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void getWarehouseByIdShouldReturnWarehouse() throws Exception {
        // Arrange
        WarehouseResponseDTO response =
                new WarehouseResponseDTO(
                        1L,
                        "Main Warehouse",
                        "Vilnius"
                );

        when(warehouseService.getWarehouseById(1L))
                .thenReturn(response);

        // Act + Assert
        mockMvc.perform(get("/warehouses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Main Warehouse"))
                .andExpect(jsonPath("$.address").value("Vilnius"));

        verify(warehouseService).getWarehouseById(1L);
    }

    @Test
    void getAllWarehousesShouldReturnWarehouses() throws Exception {
        // Arrange
        WarehouseResponseDTO response =
                new WarehouseResponseDTO(
                        1L,
                        "Main Warehouse",
                        "Vilnius"
                );

        when(warehouseService.getAllWarehouses())
                .thenReturn(List.of(response));

        // Act + Assert
        mockMvc.perform(get("/warehouses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name")
                        .value("Main Warehouse"));

        verify(warehouseService).getAllWarehouses();
    }

    @Test
    void createWarehouseShouldReturnCreatedAndId() throws Exception {
        // Arrange
        when(warehouseService.createWarehouse(
                any(WarehouseRequestDTO.class)))
                .thenReturn(1L);

        String json = """
                {
                    "name": "Main Warehouse",
                    "address": "Vilnius"
                }
                """;

        // Act + Assert
        mockMvc.perform(post("/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().string("1"));

        verify(warehouseService)
                .createWarehouse(any(WarehouseRequestDTO.class));
    }

    @Test
    void updateWarehouseShouldReturnUpdatedWarehouse() throws Exception {
        // Arrange
        WarehouseResponseDTO response =
                new WarehouseResponseDTO(
                        1L,
                        "Updated Warehouse",
                        "Kaunas"
                );

        when(warehouseService.updateWarehouse(
                eq(1L),
                any(WarehouseRequestDTO.class)
        )).thenReturn(response);

        String json = """
                {
                    "name": "Updated Warehouse",
                    "address": "Kaunas"
                }
                """;

        // Act + Assert
        mockMvc.perform(put("/warehouses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name")
                        .value("Updated Warehouse"))
                .andExpect(jsonPath("$.address")
                        .value("Kaunas"));

        verify(warehouseService).updateWarehouse(
                eq(1L),
                any(WarehouseRequestDTO.class)
        );
    }

    @Test
    void deleteWarehouseShouldReturnNoContent() throws Exception {
        // Act + Assert
        mockMvc.perform(delete("/warehouses/1"))
                .andExpect(status().isNoContent());

        verify(warehouseService).deleteWarehouse(1L);
    }
}