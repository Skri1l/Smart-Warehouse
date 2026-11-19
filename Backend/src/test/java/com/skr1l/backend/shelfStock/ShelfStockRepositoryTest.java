package com.skr1l.backend.shelfStock;

import com.skr1l.backend.product.Product;
import com.skr1l.backend.rack.Rack;
import com.skr1l.backend.shelf.Shelf;
import com.skr1l.backend.shelf.enums.ShelfStatus;
import com.skr1l.backend.shelfStock.enums.StockStatus;
import com.skr1l.backend.shelfStock.repository.ShelfStockRepository;
import com.skr1l.backend.warehouse.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ShelfStockRepositoryTest {

    @Autowired
    private ShelfStockRepository shelfStockRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Shelf shelf;

    private Product product;

    @BeforeEach
    void setUp() {
        Warehouse warehouse = new Warehouse();
        warehouse.setName("Stock test warehouse");
        warehouse.setAddress("Stock test address");

        entityManager.persist(warehouse);

        Rack rack = new Rack();
        rack.setLabel("STOCK-RACK");
        rack.setWarehouse(warehouse);

        entityManager.persist(rack);

        shelf = new Shelf();
        shelf.setLabel("STOCK-SHELF");
        shelf.setCapacity(100);
        shelf.setStatus(ShelfStatus.ACTIVE);
        shelf.setRack(rack);

        entityManager.persist(shelf);

        product = new Product();
        product.setSku("STOCK-PRODUCT-001");
        product.setName("Coca-Cola");

        entityManager.persist(product);

        entityManager.flush();
    }

    @Test
    void findByShelfIdShouldReturnShelfStock() {
        // Arrange
        ShelfStock stock = createStock(LocalDate.now().plusDays(10));

        shelfStockRepository.save(stock);

        // Act
        List<ShelfStock> result = shelfStockRepository.findByShelfId(shelf.getId());

        // Assert
        assertEquals(1, result.size());
        assertEquals(shelf.getId(), result.getFirst().getShelf().getId());
    }

    @Test
    void findByProductIdShouldReturnShelfStock() {
        // Arrange
        ShelfStock stock = createStock(LocalDate.now().plusDays(10));

        shelfStockRepository.save(stock);

        // Act
        List<ShelfStock> result = shelfStockRepository.findByProductId(product.getId());

        // Assert
        assertEquals(1, result.size());
        assertEquals(product.getId(), result.getFirst().getProduct().getId());
    }

    @Test
    void findByExpirationDateShouldReturnShelfStock() {
        // Arrange
        LocalDate expirationDate = LocalDate.now().plusDays(7);

        ShelfStock stock = createStock(expirationDate);

        shelfStockRepository.save(stock);

        // Act
        List<ShelfStock> result = shelfStockRepository.findByExpirationDate(expirationDate);

        // Assert
        assertEquals(1, result.size());
        assertEquals(expirationDate, result.getFirst().getExpirationDate());
    }

    @Test
    void findByExpirationDateBeforeShouldReturnExpiredStock() {
        // Arrange
        ShelfStock expiredStock = createStock(LocalDate.now().minusDays(2));

        expiredStock.setStatus(StockStatus.EXPIRED);

        shelfStockRepository.save(expiredStock);

        // Act
        List<ShelfStock> result = shelfStockRepository.findByExpirationDateBefore(LocalDate.now());

        // Assert
        assertFalse(result.isEmpty());

        assertTrue(result.stream().anyMatch(
                stock -> stock.getId().equals(expiredStock.getId())));
    }

    private ShelfStock createStock(LocalDate expirationDate) {
        ShelfStock stock = new ShelfStock();

        stock.setShelf(shelf);
        stock.setProduct(product);
        stock.setQuantity(25);
        stock.setExpirationDate(expirationDate);
        stock.setStatus(StockStatus.ACTIVE);

        return stock;
    }
}
