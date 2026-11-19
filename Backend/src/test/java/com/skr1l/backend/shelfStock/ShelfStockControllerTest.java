package com.skr1l.backend.shelfStock;

import com.skr1l.backend.shelfStock.controller.ShelfStockController;
import com.skr1l.backend.shelfStock.dto.ShelfStockRequestDto;
import com.skr1l.backend.shelfStock.dto.ShelfStockResponseDto;
import com.skr1l.backend.shelfStock.enums.StockStatus;
import com.skr1l.backend.shelfStock.service.ShelfStockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDate;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ShelfStockControllerTest {

    @Mock
    private ShelfStockService shelfStockService;

    private MockMvc mockMvc;

    private LocalDate expirationDate;

    @BeforeEach
    void setUp() {
        expirationDate = LocalDate.now().plusDays(10);

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new ShelfStockController(shelfStockService)
                )
                .build();
    }

    @Test
    void getShelfStockByIdShouldReturnStock() throws Exception {
        when(shelfStockService.getShelfStockById(40L))
                .thenReturn(new ShelfStockResponseDto(
                        40L,
                        20L,
                        10L,
                        25,
                        expirationDate,
                        StockStatus.ACTIVE
                ));

        mockMvc.perform(get("/shelf-stock/40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(40))
                .andExpect(jsonPath("$.shelfId").value(20))
                .andExpect(jsonPath("$.productId").value(10))
                .andExpect(jsonPath("$.quantity").value(25))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getAllShelfStockShouldReturnStock() throws Exception {
        when(shelfStockService.getAllShelfStock())
                .thenReturn(List.of(
                        new ShelfStockResponseDto(
                                40L,
                                20L,
                                10L,
                                25,
                                expirationDate,
                                StockStatus.ACTIVE
                        )
                ));

        mockMvc.perform(get("/shelf-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity")
                        .value(25));
    }

    @Test
    void getShelfStockByShelfIdShouldReturnStock() throws Exception {
        when(shelfStockService.getShelfStockByShelfId(20L))
                .thenReturn(List.of(
                        new ShelfStockResponseDto(
                                40L,
                                20L,
                                10L,
                                25,
                                expirationDate,
                                StockStatus.ACTIVE
                        )
                ));

        mockMvc.perform(
                        get("/shelf-stock/shelf/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shelfId")
                        .value(20));
    }

    @Test
    void getShelfStockByProductIdShouldReturnStock() throws Exception {
        when(shelfStockService.getShelfStockByProductId(10L))
                .thenReturn(List.of(
                        new ShelfStockResponseDto(
                                40L,
                                20L,
                                10L,
                                25,
                                expirationDate,
                                StockStatus.ACTIVE
                        )
                ));

        mockMvc.perform(
                        get("/shelf-stock/product/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId")
                        .value(10));
    }

    @Test
    void getShelfStockByExpirationDateShouldReturnStock() throws Exception {
        when(shelfStockService
                .getShelfStockByExpirationDate(expirationDate))
                .thenReturn(List.of(
                        new ShelfStockResponseDto(
                                40L,
                                20L,
                                10L,
                                25,
                                expirationDate,
                                StockStatus.ACTIVE
                        )
                ));

        mockMvc.perform(
                        get("/shelf-stock/expiration-date")
                                .param(
                                        "date",
                                        expirationDate.toString()
                                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity")
                        .value(25));
    }

    @Test
    void getExpiredShelfStockShouldReturnExpiredStock() throws Exception {
        LocalDate expiredDate =
                LocalDate.now().minusDays(1);

        when(shelfStockService.getExpiredShelfStock())
                .thenReturn(List.of(
                        new ShelfStockResponseDto(
                                41L,
                                20L,
                                10L,
                                5,
                                expiredDate,
                                StockStatus.EXPIRED
                        )
                ));

        mockMvc.perform(
                        get("/shelf-stock/expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status")
                        .value("EXPIRED"));
    }

    @Test
    void createShelfStockShouldReturnCreatedAndId() throws Exception {
        when(shelfStockService.createShelfStock(
                any(ShelfStockRequestDto.class)))
                .thenReturn(40L);

        String json = """
                {
                    "shelfId": 20,
                    "productId": 10,
                    "quantity": 25,
                    "expirationDate": "%s"
                }
                """.formatted(expirationDate);

        mockMvc.perform(post("/shelf-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().string("40"));
    }

    @Test
    void updateShelfStockShouldReturnUpdatedStock() throws Exception {
        when(shelfStockService.updateShelfStock(
                eq(40L),
                any(ShelfStockRequestDto.class)))
                .thenReturn(new ShelfStockResponseDto(
                        40L,
                        20L,
                        10L,
                        50,
                        expirationDate,
                        StockStatus.ACTIVE
                ));

        String json = """
                {
                    "shelfId": 20,
                    "productId": 10,
                    "quantity": 50,
                    "expirationDate": "%s"
                }
                """.formatted(expirationDate);

        mockMvc.perform(put("/shelf-stock/40")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity")
                        .value(50));
    }

    @Test
    void deleteShelfStockShouldReturnNoContent() throws Exception {
        mockMvc.perform(
                        delete("/shelf-stock/40"))
                .andExpect(status().isNoContent());

        verify(shelfStockService)
                .deleteShelfStock(40L);
    }
}