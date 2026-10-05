package com.skr1l.backend.shelf;

import com.skr1l.backend.exception.rackExceptions.RackIsNotFoundException;
import com.skr1l.backend.exception.shelfExceptions.ShelfIsNotFoundException;
import com.skr1l.backend.rack.Rack;
import com.skr1l.backend.rack.repository.RackRepository;
import com.skr1l.backend.shelf.dto.ShelfRequestDto;
import com.skr1l.backend.shelf.dto.ShelfResponseDto;
import com.skr1l.backend.shelf.enums.ShelfStatus;
import com.skr1l.backend.shelf.repository.ShelfRepository;
import com.skr1l.backend.shelf.service.ShelfServiceImpl;
import com.skr1l.backend.warehouse.Warehouse;
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
class ShelfServiceTest {

    @Mock
    private ShelfRepository shelfRepository;

    @Mock
    private RackRepository rackRepository;

    @InjectMocks
    private ShelfServiceImpl shelfService;

    private Rack rack;

    private Shelf shelf;

    private ShelfRequestDto dto;

    @BeforeEach
    void setUp() {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(1L);

        rack = new Rack();
        rack.setId(10L);
        rack.setLabel("Rack A");
        rack.setWarehouse(warehouse);

        shelf = new Shelf();
        shelf.setId(20L);
        shelf.setLabel("Shelf A1");
        shelf.setCapacity(100);
        shelf.setStatus(ShelfStatus.ACTIVE);
        shelf.setRack(rack);

        dto = new ShelfRequestDto("Shelf A1", 100, 10L);
    }

    @Test
    void createShelfShouldReturnIdWhenRackExists() {
        // Arrange
        when(rackRepository.findById(10L))
                .thenReturn(Optional.of(rack));

        when(shelfRepository.save(any(Shelf.class)))
                .thenAnswer(invocation -> {
                    Shelf savedShelf = invocation.getArgument(0);
                    savedShelf.setId(20L);
                    return savedShelf;
                });

        // Act
        Long result = shelfService.createShelf(dto);

        // Assert
        assertEquals(20L, result);

        verify(rackRepository).findById(10L);
        verify(shelfRepository).save(any(Shelf.class));
    }

    @Test
    void createShelfShouldThrowExceptionWhenRackDoesNotExist() {
        // Arrange
        when(rackRepository.findById(10L))
                .thenReturn(Optional.empty());

        // Act
        // Assert
        assertThrows(RackIsNotFoundException.class, () -> shelfService.createShelf(dto));

        verify(shelfRepository, never()).save(any(Shelf.class));
    }

    @Test
    void getShelfByIdShouldReturnShelfWhenShelfExists() {
        // Arrange
        when(shelfRepository.findById(20L))
                .thenReturn(Optional.of(shelf));

        // Act
        ShelfResponseDto result = shelfService.getShelfById(20L);

        // Assert
        assertEquals(20L, result.id());
        assertEquals("Shelf A1", result.label());
        assertEquals(100, result.capacity());
        assertEquals(ShelfStatus.ACTIVE, result.status());
        assertEquals(10L, result.rackId());
    }

    @Test
    void getShelfByIdShouldThrowExceptionWhenShelfDoesNotExist() {
        // Arrange
        when(shelfRepository.findById(20L))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(ShelfIsNotFoundException.class, () -> shelfService.getShelfById(20L));
    }

    @Test
    void getAllShelvesShouldReturnShelves() {
        // Arrange
        when(shelfRepository.findAll())
                .thenReturn(List.of(shelf));

        // Act
        List<ShelfResponseDto> result =
                shelfService.getAllShelves();

        // Assert
        assertEquals(1, result.size());
        assertEquals("Shelf A1", result.getFirst().label());
    }

    @Test
    void updateShelfShouldUpdateShelf() {
        // Arrange
        ShelfRequestDto updateDto = new ShelfRequestDto("Shelf B1", 200, 10L);

        when(shelfRepository.findById(20L))
                .thenReturn(Optional.of(shelf));

        when(rackRepository.findById(10L))
                .thenReturn(Optional.of(rack));

        // Act
        ShelfResponseDto result = shelfService.updateShelf(20L, updateDto);

        // Assert
        assertEquals("Shelf B1", result.label());
        assertEquals(200, result.capacity());

        assertEquals("Shelf B1", shelf.getLabel());
        assertEquals(200, shelf.getCapacity());
    }

    @Test
    void activateShelfShouldSetStatusActive() {
        // Arrange
        shelf.setStatus(ShelfStatus.INACTIVE);

        when(shelfRepository.findById(20L))
                .thenReturn(Optional.of(shelf));

        // Act
        shelfService.activateShelf(20L);

        // Assert
        assertEquals(ShelfStatus.ACTIVE, shelf.getStatus());
    }

    @Test
    void deactivateShelfShouldSetStatusInactive() {
        // Arrange
        when(shelfRepository.findById(20L))
                .thenReturn(Optional.of(shelf));

        // Act
        shelfService.deactivateShelf(20L);

        // Assert
        assertEquals(ShelfStatus.INACTIVE, shelf.getStatus());
    }

    @Test
    void deleteShelfShouldDeleteShelfWhenShelfExists() {
        // Arrange
        when(shelfRepository.findById(20L))
                .thenReturn(Optional.of(shelf));

        // Act
        shelfService.deleteShelfById(20L);

        // Assert
        verify(shelfRepository).delete(shelf);
    }
}