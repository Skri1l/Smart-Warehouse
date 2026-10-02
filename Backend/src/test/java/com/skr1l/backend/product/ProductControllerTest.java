package com.skr1l.backend.product;

import com.skr1l.backend.product.controller.ProductController;
import com.skr1l.backend.product.dto.ProductRequestDto;
import com.skr1l.backend.product.dto.ProductResponseDto;
import com.skr1l.backend.product.service.ProductService;
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
class ProductControllerTest {

    @Mock
    private ProductService productService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new ProductController(productService)
                )
                .build();
    }

    @Test
    void getProductByIdShouldReturnProduct() throws Exception {
        when(productService.getProductById(10L))
                .thenReturn(new ProductResponseDto(
                        10L,
                        "COLA-001",
                        "Coca-Cola",
                        "Coca-Cola 0.5L"
                ));

        mockMvc.perform(get("/products/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.sku").value("COLA-001"))
                .andExpect(jsonPath("$.name").value("Coca-Cola"));
    }

    @Test
    void getProductBySkuShouldReturnProduct() throws Exception {
        when(productService.getProductBySku("COLA-001"))
                .thenReturn(new ProductResponseDto(
                        10L,
                        "COLA-001",
                        "Coca-Cola",
                        "Coca-Cola 0.5L"
                ));

        mockMvc.perform(
                        get("/products/sku/COLA-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku")
                        .value("COLA-001"));
    }

    @Test
    void getProductsByNameShouldReturnProducts() throws Exception {
        when(productService.getProductsByName("cola"))
                .thenReturn(List.of(
                        new ProductResponseDto(
                                10L,
                                "COLA-001",
                                "Coca-Cola",
                                "Description"
                        )
                ));

        mockMvc.perform(
                        get("/products/search")
                                .param("name", "cola"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name")
                        .value("Coca-Cola"));
    }

    @Test
    void getAllProductsShouldReturnProducts() throws Exception {
        when(productService.getAllProducts())
                .thenReturn(List.of(
                        new ProductResponseDto(
                                10L,
                                "COLA-001",
                                "Coca-Cola",
                                "Description"
                        )
                ));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku")
                        .value("COLA-001"));
    }

    @Test
    void createProductShouldReturnCreatedAndId() throws Exception {
        when(productService.createProduct(
                any(ProductRequestDto.class)))
                .thenReturn(10L);

        String json = """
                {
                    "sku": "COLA-001",
                    "name": "Coca-Cola",
                    "description": "Coca-Cola 0.5L"
                }
                """;

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().string("10"));
    }

    @Test
    void updateProductShouldReturnUpdatedProduct() throws Exception {
        when(productService.updateProduct(
                eq(10L),
                any(ProductRequestDto.class)))
                .thenReturn(new ProductResponseDto(
                        10L,
                        "COLA-002",
                        "Coca-Cola Zero",
                        "Zero sugar"
                ));

        String json = """
                {
                    "sku": "COLA-002",
                    "name": "Coca-Cola Zero",
                    "description": "Zero sugar"
                }
                """;

        mockMvc.perform(put("/products/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku")
                        .value("COLA-002"))
                .andExpect(jsonPath("$.name")
                        .value("Coca-Cola Zero"));
    }

    @Test
    void deleteProductShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/products/10"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(10L);
    }
}