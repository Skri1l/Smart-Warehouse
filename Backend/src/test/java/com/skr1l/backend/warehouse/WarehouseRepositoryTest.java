package com.skr1l.backend.warehouse;

import com.skr1l.backend.warehouse.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class WarehouseRepositoryTest {

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Test
    void findByIdShouldReturnWarehouseWhenWarehouseExists() {
        // Arrange
        Warehouse warehouse = new Warehouse();
        warehouse.setName("Main Warehouse");
        warehouse.setAddress("Vilnius");

        Warehouse savedWarehouse = warehouseRepository.save(warehouse);

        // Act
        Optional<Warehouse> result = warehouseRepository.findById(savedWarehouse.getId());

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Main Warehouse", result.get().getName());
        assertEquals("Vilnius", result.get().getAddress());
    }

    @Test
    void existsByNameAndAddressShouldReturnTrueWhenWarehouseExists() {
        // Arrange
        Warehouse warehouse = new Warehouse();
        warehouse.setName("Main Warehouse");
        warehouse.setAddress("Vilnius");

        warehouseRepository.save(warehouse);

        // Act
        boolean result = warehouseRepository.existsByNameAndAddress(
                "Main Warehouse",
                "Vilnius");

        // Assert
        assertTrue(result);
    }

    @Test
    void existsByNameAndAddressShouldReturnFalseWhenWarehouseDoesNotExist() {
        // Act
        boolean result = warehouseRepository.existsByNameAndAddress(
                "Unknown Warehouse",
                "Unknown Address");

        // Assert
        assertFalse(result);
    }
}