package com.skr1l.backend.shelf;

import com.skr1l.backend.shelf.controller.ShelfController;
import com.skr1l.backend.shelf.dto.ShelfRequestDto;
import com.skr1l.backend.shelf.dto.ShelfResponseDto;
import com.skr1l.backend.shelf.enums.ShelfStatus;
import com.skr1l.backend.shelf.service.ShelfService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ShelfControllerTest {

    @Mock
    private ShelfService shelfService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ShelfController controller =
                new ShelfController(shelfService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void getShelfByIdShouldReturnShelf() throws Exception {
        // Arrange
        when(shelfService.getShelfById(20L)).thenReturn(
                new ShelfResponseDto(
                        20L, "Shelf A1", 100, ShelfStatus.ACTIVE, 10L));

        // Act
        // Assert
        mockMvc.perform(get("/shelves/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.label").value("Shelf A1"))
                .andExpect(jsonPath("$.capacity").value(100))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.rackId").value(10));
    }

    @Test
    void getAllShelvesShouldReturnShelves() throws Exception {
        // Arrange
        when(shelfService.getAllShelves()).thenReturn(List.of(new ShelfResponseDto(
                20L, "Shelf A1", 100, ShelfStatus.ACTIVE, 10L)));

        // Act + Assert
        mockMvc.perform(get("/shelves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label")
                        .value("Shelf A1"));
    }

    @Test
    void createShelfShouldReturnCreatedAndId() throws Exception {
        // Arrange
        when(shelfService.createShelf(any(ShelfRequestDto.class))).thenReturn(20L);

        String json = """
                {
                    "label": "Shelf A1",
                    "capacity": 100,
                    "rackId": 10
                }
                """;

        // Act
        // Assert
        mockMvc.perform(post("/shelves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().string("20"));
    }

    @Test
    void updateShelfShouldReturnUpdatedShelf() throws Exception {
        // Arrange
        when(shelfService.updateShelf(eq(20L), any(ShelfRequestDto.class)))
                .thenReturn(new ShelfResponseDto(
                        20L, "Shelf B1", 200, ShelfStatus.ACTIVE, 10L));

        String json = """
                {
                    "label": "Shelf B1",
                    "capacity": 200,
                    "rackId": 10
                }
                """;

        // Act
        // Assert
        mockMvc.perform(put("/shelves/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label")
                        .value("Shelf B1"))
                .andExpect(jsonPath("$.capacity")
                        .value(200));
    }

    @Test
    void activateShelfShouldReturnNoContent() throws Exception {
        mockMvc.perform(
                        patch("/shelves/20/activate")
                )
                .andExpect(status().isNoContent());

        verify(shelfService).activateShelf(20L);
    }

    @Test
    void deactivateShelfShouldReturnNoContent() throws Exception {
        mockMvc.perform(patch("/shelves/20/deactivate"))
                .andExpect(status().isNoContent());

        verify(shelfService).deactivateShelf(20L);
    }

    @Test
    void deleteShelfShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/shelves/20"))
                .andExpect(status().isNoContent());

        verify(shelfService).deleteShelfById(20L);
    }
}