package com.skr1l.backend.product;

import com.skr1l.backend.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void findBySkuShouldReturnProductWhenSkuExists() {
        // Arrange
        Product product = new Product();
        product.setSku("COLA-TEST-001");
        product.setName("Coca-Cola");
        product.setDescription("Test");

        productRepository.save(product);

        // Act
        Optional<Product> result = productRepository.findBySku("COLA-TEST-001");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Coca-Cola", result.get().getName());
    }

    @Test
    void findByNameContainingIgnoreCaseShouldReturnProducts() {
        // Arrange
        Product product = new Product();
        product.setSku("COLA-TEST-002");
        product.setName("Coca-Cola Zero");

        productRepository.save(product);

        // Act
        List<Product> result = productRepository.findByNameContainingIgnoreCase("cola");

        // Assert
        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(p -> p.getSku().equals("COLA-TEST-002")));
    }

    @Test
    void existsBySkuShouldReturnTrueWhenSkuExists() {
        // Arrange
        Product product = new Product();
        product.setSku("COLA-TEST-003");
        product.setName("Coca-Cola");

        productRepository.save(product);

        // Act
        boolean result = productRepository.existsBySku("COLA-TEST-003");

        // Assert
        assertTrue(result);
    }
}