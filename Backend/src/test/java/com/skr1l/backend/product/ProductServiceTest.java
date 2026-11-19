package com.skr1l.backend.product;

import com.skr1l.backend.exception.productExceptions.ProductAlreadyExistsException;
import com.skr1l.backend.exception.productExceptions.ProductIsNotFoundException;
import com.skr1l.backend.product.dto.ProductRequestDto;
import com.skr1l.backend.product.dto.ProductResponseDto;
import com.skr1l.backend.product.repository.ProductRepository;
import com.skr1l.backend.product.service.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;

    private ProductRequestDto dto;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(10L);
        product.setSku("COLA-001");
        product.setName("Coca-Cola");
        product.setDescription("Coca-Cola 0.5L");

        dto = new ProductRequestDto(
                "COLA-001", "Coca-Cola", "Coca-Cola 0.5L");
    }

    @Test
    void createProductShouldReturnIdWhenSkuIsUnique() {
        // Arrange
        when(productRepository.existsBySku("COLA-001"))
                .thenReturn(false);

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product savedProduct = invocation.getArgument(0);
                    savedProduct.setId(10L);
                    return savedProduct;
                });

        // Act
        Long result = productService.createProduct(dto);

        // Assert
        assertEquals(10L, result);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProductShouldThrowExceptionWhenSkuExists() {
        // Arrange
        when(productRepository.existsBySku("COLA-001"))
                .thenReturn(true);

        // Act
        // Assert
        assertThrows(ProductAlreadyExistsException.class,
                () -> productService.createProduct(dto));

        verify(productRepository, never())
                .save(any(Product.class));
    }

    @Test
    void getProductByIdShouldReturnProduct() {
        // Arrange
        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));

        // Act
        ProductResponseDto result = productService.getProductById(10L);

        // Assert
        assertEquals(10L, result.id());
        assertEquals("COLA-001", result.sku());
        assertEquals("Coca-Cola", result.name());
    }

    @Test
    void getProductByIdShouldThrowExceptionWhenProductDoesNotExist() {
        // Arrange
        when(productRepository.findById(10L))
                .thenReturn(Optional.empty());

        // Act
        // Assert
        assertThrows(ProductIsNotFoundException.class,
                () -> productService.getProductById(10L));
    }

    @Test
    void getProductBySkuShouldReturnProduct() {
        // Arrange
        when(productRepository.findBySku("COLA-001"))
                .thenReturn(Optional.of(product));

        // Act
        ProductResponseDto result =
                productService.getProductBySku("COLA-001");

        // Assert
        assertEquals(10L, result.id());
        assertEquals("COLA-001", result.sku());
    }

    @Test
    void getProductBySkuShouldThrowExceptionWhenProductDoesNotExist() {
        // Arrange
        when(productRepository.findBySku("COLA-001"))
                .thenReturn(Optional.empty());

        // Act
        // Assert
        assertThrows(ProductIsNotFoundException.class,
                () -> productService.getProductBySku("COLA-001"));
    }

    @Test
    void getProductsByNameShouldReturnMatchingProducts() {
        // Arrange
        when(productRepository.findByNameContainingIgnoreCase("cola"))
                .thenReturn(List.of(product));

        // Act
        List<ProductResponseDto> result =
                productService.getProductsByName("cola");

        // Assert
        assertEquals(1, result.size());
        assertEquals("Coca-Cola", result.getFirst().name());
    }

    @Test
    void getAllProductsShouldReturnProducts() {
        // Arrange
        when(productRepository.findAll())
                .thenReturn(List.of(product));

        // Act
        List<ProductResponseDto> result =
                productService.getAllProducts();

        // Assert
        assertEquals(1, result.size());
        assertEquals("COLA-001", result.getFirst().sku());
    }

    @Test
    void updateProductShouldUpdateProduct() {
        // Arrange
        ProductRequestDto updateDto = new ProductRequestDto(
                        "COLA-002", "Coca-Cola Zero", "Zero sugar");

        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));

        when(productRepository.existsBySku("COLA-002"))
                .thenReturn(false);

        // Act
        ProductResponseDto result =
                productService.updateProduct(10L, updateDto);

        // Assert
        assertEquals("COLA-002", result.sku());
        assertEquals("Coca-Cola Zero", result.name());

        assertEquals("COLA-002", product.getSku());
        assertEquals("Coca-Cola Zero", product.getName());
    }

    @Test
    void updateProductShouldThrowExceptionWhenSkuExists() {
        // Arrange
        ProductRequestDto updateDto = new ProductRequestDto(
                "PEPSI-001", "Coca-Cola", "Description");

        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));

        when(productRepository.existsBySku("PEPSI-001"))
                .thenReturn(true);

        // Act
        // Assert
        assertThrows(ProductAlreadyExistsException.class,
                () -> productService.updateProduct(10L, updateDto));
    }

    @Test
    void deleteProductShouldDeleteProduct() {
        // Arrange
        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));

        // Act
        productService.deleteProduct(10L);

        // Assert
        verify(productRepository).delete(product);
    }
}