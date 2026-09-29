package com.skr1l.backend.rack;

import com.skr1l.backend.exception.rackExceptions.RackIsNotFoundException;
import com.skr1l.backend.exception.warehouseExceptions.WarehouseIsNotFoundException;
import com.skr1l.backend.rack.dto.RackRequestDto;
import com.skr1l.backend.rack.dto.RackResponseDto;
import com.skr1l.backend.rack.repository.RackRepository;
import com.skr1l.backend.rack.service.RackServiceImpl;
import com.skr1l.backend.warehouse.Warehouse;
import com.skr1l.backend.warehouse.repository.WarehouseRepository;
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
class RackServiceTest {

    @Mock
    private RackRepository rackRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private RackServiceImpl rackService;

    private Warehouse warehouse;
    private Rack rack;
    private RackRequestDto dto;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setName("Main Warehouse");
        warehouse.setAddress("Vilnius");

        rack = new Rack();
        rack.setId(10L);
        rack.setLabel("Rack A");
        rack.setWarehouse(warehouse);

        dto = new RackRequestDto("Rack A", 1L);
    }

    @Test
    void createRackShouldReturnIdWhenWarehouseExists() {
        // Arrange
        when(warehouseRepository.findById(1L))
                .thenReturn(Optional.of(warehouse));

        when(rackRepository.save(any(Rack.class)))
                .thenAnswer(invocation -> {
                    Rack savedRack = invocation.getArgument(0);
                    savedRack.setId(10L);
                    return savedRack;
                });

        // Act
        Long result = rackService.createRack(dto, 1L);

        // Assert
        assertEquals(10L, result);

        verify(warehouseRepository).findById(1L);
        verify(rackRepository).save(any(Rack.class));
    }

    @Test
    void createRackShouldThrowExceptionWhenWarehouseDoesNotExist() {
        // Arrange
        when(warehouseRepository.findById(1L))
                .thenReturn(Optional.empty());

        // Act
        // Assert
        assertThrows(WarehouseIsNotFoundException.class, () -> rackService.createRack(dto, 1L));

        verify(rackRepository, never()).save(any(Rack.class));
    }

    @Test
    void getRackByIdShouldReturnRackWhenRackExists() {
        // Arrange
        when(rackRepository.findById(10L))
                .thenReturn(Optional.of(rack));

        // Act
        RackResponseDto result = rackService.getRackById(10L);

        // Assert
        assertEquals(10L, result.id());
        assertEquals("Rack A", result.label());
        assertEquals(1L, result.warehouseId());
    }

    @Test
    void getRackByIdShouldThrowExceptionWhenRackDoesNotExist() {
        // Arrange
        when(rackRepository.findById(10L))
                .thenReturn(Optional.empty());

        // Act
        // Assert
        assertThrows(RackIsNotFoundException.class, () -> rackService.getRackById(10L));
    }

    @Test
    void getAllRacksShouldReturnRacks() {
        // Arrange
        when(rackRepository.findAll())
                .thenReturn(List.of(rack));

        // Act
        List<RackResponseDto> result =
                rackService.getAllRacks();

        // Assert
        assertEquals(1, result.size());
        assertEquals("Rack A", result.getFirst().label());
    }

    @Test
    void updateRackShouldUpdateRack() {
        // Arrange
        RackRequestDto updateDto =
                new RackRequestDto("Rack B", 1L);

        when(rackRepository.findById(10L))
                .thenReturn(Optional.of(rack));

        when(warehouseRepository.findById(1L))
                .thenReturn(Optional.of(warehouse));

        // Act
        RackResponseDto result = rackService.updateRack(updateDto, 10L, 1L);

        // Assert
        assertEquals("Rack B", result.label());
        assertEquals("Rack B", rack.getLabel());
        assertEquals(warehouse, rack.getWarehouse());
    }

    @Test
    void deleteRackShouldDeleteRackWhenRackExists() {
        // Arrange
        when(rackRepository.findById(10L))
                .thenReturn(Optional.of(rack));

        // Act
        rackService.deleteRackById(10L);

        // Assert
        verify(rackRepository).delete(rack);
    }
}