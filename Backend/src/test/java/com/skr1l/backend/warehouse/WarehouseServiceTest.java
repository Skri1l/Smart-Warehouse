package com.skr1l.backend.warehouse;

import com.skr1l.backend.exception.warehouseExceptions.WarehouseAlreadyExistsException;
import com.skr1l.backend.exception.warehouseExceptions.WarehouseIsNotFoundException;
import com.skr1l.backend.warehouse.dto.WarehouseRequestDTO;
import com.skr1l.backend.warehouse.dto.WarehouseResponseDTO;
import com.skr1l.backend.warehouse.repository.WarehouseRepository;
import com.skr1l.backend.warehouse.service.WarehouseServiceImpl;
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
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private WarehouseServiceImpl warehouseService;

    private Warehouse warehouse;

    private WarehouseRequestDTO dto;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setName("Main Warehouse");
        warehouse.setAddress("Vilnius");

        dto = new WarehouseRequestDTO(
                "Main Warehouse",
                "Vilnius"
        );
    }

    @Test
    void createWarehouseShouldReturnId() {
        // Arrange
        when(warehouseRepository.existsByNameAndAddress(dto.name(), dto.address()))
                .thenReturn(false);

        when(warehouseRepository.save(any(Warehouse.class)))
                .thenAnswer(invocation -> {
                    Warehouse savedWarehouse = invocation.getArgument(0);
                    savedWarehouse.setId(1L);
                    return savedWarehouse;
                });

        // Act
        Long result = warehouseService.createWarehouse(dto);

        // Assert
        assertEquals(1L, result);

        verify(warehouseRepository).existsByNameAndAddress(dto.name(), dto.address());

        verify(warehouseRepository).save(any(Warehouse.class));
    }

    @Test
    void createWarehouseShouldThrowExceptionWhenWarehouseAlreadyExists() {
        // Arrange
        when(warehouseRepository.existsByNameAndAddress(dto.name(), dto.address())).thenReturn(true);

        // Act
        // Assert
        assertThrows(WarehouseAlreadyExistsException.class,
                () -> warehouseService.createWarehouse(dto));

        verify(warehouseRepository).existsByNameAndAddress(dto.name(), dto.address());

        verify(warehouseRepository, never()).save(any(Warehouse.class));
    }

    @Test
    void getWarehouseByIdShouldReturnWarehouseWhenWarehouseExists() {
        // Arrange
        when(warehouseRepository.findById(1L))
                .thenReturn(Optional.of(warehouse));

        // Act
        WarehouseResponseDTO result =
                warehouseService.getWarehouseById(1L);

        // Assert
        assertEquals(1L, result.id());
        assertEquals("Main Warehouse", result.name());
        assertEquals("Vilnius", result.address());

        verify(warehouseRepository).findById(1L);
    }

    @Test
    void getWarehouseByIdShouldThrowExceptionWhenWarehouseDoesNotExist() {
        // Arrange
        when(warehouseRepository.findById(1L))
                .thenReturn(Optional.empty());

        // Act
        // Assert
        assertThrows(WarehouseIsNotFoundException.class,
                () -> warehouseService.getWarehouseById(1L));

        verify(warehouseRepository).findById(1L);
    }

    @Test
    void getAllWarehousesShouldReturnWarehouses() {
        // Arrange
        when(warehouseRepository.findAll())
                .thenReturn(List.of(warehouse));

        // Act
        List<WarehouseResponseDTO> result = warehouseService.getAllWarehouses();

        // Assert
        assertEquals(1, result.size());
        assertEquals("Main Warehouse", result.getFirst().name());
        assertEquals("Vilnius", result.getFirst().address());

        verify(warehouseRepository).findAll();
    }

    @Test
    void updateWarehouseShouldReturnUpdatedWarehouse() {
        // Arrange
        WarehouseRequestDTO updateDto = new WarehouseRequestDTO(
                "Updated Warehouse",
                "Kaunas");

        when(warehouseRepository.findById(1L))
                .thenReturn(Optional.of(warehouse));

        when(warehouseRepository.existsByNameAndAddressAndIdNot(updateDto.name(),
                updateDto.address(), 1L))
                .thenReturn(false);

        // Act
        WarehouseResponseDTO result =
                warehouseService.updateWarehouse(1L, updateDto);

        // Assert
        assertEquals("Updated Warehouse", result.name());
        assertEquals("Kaunas", result.address());

        verify(warehouseRepository)
                .existsByNameAndAddressAndIdNot(updateDto.name(), updateDto.address(), 1L);
    }

    @Test
    void updateWarehouseShouldThrowExceptionWhenWarehouseAlreadyExists() {
        // Arrange
        WarehouseRequestDTO updateDto = new WarehouseRequestDTO(
                "Updated Warehouse",
                "Kaunas");

        when(warehouseRepository.findById(1L))
                .thenReturn(Optional.of(warehouse));

        when(warehouseRepository.existsByNameAndAddressAndIdNot(updateDto.name(),
                updateDto.address(), 1L))
                .thenReturn(true);

        // Act
        // Assert
        assertThrows(WarehouseAlreadyExistsException.class,
                () -> warehouseService.updateWarehouse(1L, updateDto));

        verify(warehouseRepository)
                .existsByNameAndAddressAndIdNot(updateDto.name(), updateDto.address(), 1L);
    }

    @Test
    void deleteWarehouseShouldDeleteWarehouseWhenWarehouseExists() {
        // Arrange
        when(warehouseRepository.findById(1L))
                .thenReturn(Optional.of(warehouse));

        // Act
        warehouseService.deleteWarehouse(1L);

        // Assert
        verify(warehouseRepository).findById(1L);
        verify(warehouseRepository).delete(warehouse);
    }

    @Test
    void deleteWarehouseShouldThrowExceptionWhenWarehouseDoesNotExist() {
        // Arrange
        when(warehouseRepository.findById(1L))
                .thenReturn(Optional.empty());

        // Act
        // Assert
        assertThrows(
                WarehouseIsNotFoundException.class,
                () -> warehouseService.deleteWarehouse(1L));

        verify(warehouseRepository, never())
                .delete(any(Warehouse.class));
    }
}