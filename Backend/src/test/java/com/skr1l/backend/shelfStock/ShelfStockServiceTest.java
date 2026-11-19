package com.skr1l.backend.shelfStock;

import com.skr1l.backend.exception.productExceptions.ProductIsNotFoundException;
import com.skr1l.backend.exception.shelfExceptions.ShelfIsNotFoundException;
import com.skr1l.backend.exception.shelfStockExceptions.ShelfStockIsNotFoundException;
import com.skr1l.backend.product.Product;
import com.skr1l.backend.product.repository.ProductRepository;
import com.skr1l.backend.shelf.Shelf;
import com.skr1l.backend.shelf.repository.ShelfRepository;
import com.skr1l.backend.shelfStock.dto.ShelfStockRequestDto;
import com.skr1l.backend.shelfStock.dto.ShelfStockResponseDto;
import com.skr1l.backend.shelfStock.enums.StockStatus;
import com.skr1l.backend.shelfStock.repository.ShelfStockRepository;
import com.skr1l.backend.shelfStock.service.ShelfStockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShelfStockServiceTest {

    @Mock
    private ShelfStockRepository shelfStockRepository;

    @Mock
    private ShelfRepository shelfRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ShelfStockServiceImpl shelfStockService;

    private Shelf shelf;

    private Product product;

    private ShelfStock shelfStock;

    private LocalDate expirationDate;

    @BeforeEach
    void setUp() {
        expirationDate = LocalDate.now().plusDays(10);

        shelf = new Shelf();
        shelf.setId(20L);

        product = new Product();
        product.setId(10L);
        product.setSku("COLA-001");
        product.setName("Coca-Cola");

        shelfStock = new ShelfStock();
        shelfStock.setId(40L);
        shelfStock.setShelf(shelf);
        shelfStock.setProduct(product);
        shelfStock.setQuantity(25);
        shelfStock.setExpirationDate(expirationDate);
        shelfStock.setStatus(StockStatus.ACTIVE);
    }

    @Test
    void createShelfStockShouldReturnIdWhenDataIsValid() {
        // Arrange
        ShelfStockRequestDto dto =
                new ShelfStockRequestDto(
                        20L,
                        10L,
                        25,
                        expirationDate
                );

        when(shelfRepository.findById(20L))
                .thenReturn(Optional.of(shelf));

        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));

        when(shelfStockRepository.save(any(ShelfStock.class)))
                .thenAnswer(invocation -> {
                    ShelfStock saved = invocation.getArgument(0);
                    saved.setId(40L);
                    return saved;
                });

        // Act
        Long result =
                shelfStockService.createShelfStock(dto);

        // Assert
        assertEquals(40L, result);
        verify(shelfStockRepository)
                .save(any(ShelfStock.class));
    }

    @Test
    void createShelfStockShouldThrowExceptionWhenShelfDoesNotExist() {
        // Arrange
        ShelfStockRequestDto dto =
                new ShelfStockRequestDto(
                        20L,
                        10L,
                        25,
                        expirationDate
                );

        when(shelfRepository.findById(20L))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
                ShelfIsNotFoundException.class,
                () -> shelfStockService.createShelfStock(dto)
        );
    }

    @Test
    void createShelfStockShouldThrowExceptionWhenProductDoesNotExist() {
        // Arrange
        ShelfStockRequestDto dto =
                new ShelfStockRequestDto(
                        20L,
                        10L,
                        25,
                        expirationDate
                );

        when(shelfRepository.findById(20L))
                .thenReturn(Optional.of(shelf));

        when(productRepository.findById(10L))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
                ProductIsNotFoundException.class,
                () -> shelfStockService.createShelfStock(dto)
        );
    }

    @Test
    void getShelfStockByIdShouldReturnShelfStock() {
        // Arrange
        when(shelfStockRepository.findById(40L))
                .thenReturn(Optional.of(shelfStock));

        // Act
        ShelfStockResponseDto result =
                shelfStockService.getShelfStockById(40L);

        // Assert
        assertEquals(40L, result.id());
        assertEquals(20L, result.shelfId());
        assertEquals(10L, result.productId());
        assertEquals(25, result.quantity());
        assertEquals(StockStatus.ACTIVE, result.status());
    }

    @Test
    void getShelfStockByIdShouldThrowExceptionWhenNotFound() {
        // Arrange
        when(shelfStockRepository.findById(40L))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
                ShelfStockIsNotFoundException.class,
                () -> shelfStockService.getShelfStockById(40L)
        );
    }

    @Test
    void getAllShelfStockShouldReturnStock() {
        // Arrange
        when(shelfStockRepository.findAll())
                .thenReturn(List.of(shelfStock));

        // Act
        List<ShelfStockResponseDto> result =
                shelfStockService.getAllShelfStock();

        // Assert
        assertEquals(1, result.size());
        assertEquals(25, result.getFirst().quantity());
    }

    @Test
    void getShelfStockByShelfIdShouldReturnStock() {
        // Arrange
        when(shelfStockRepository.findByShelfId(20L))
                .thenReturn(List.of(shelfStock));

        // Act
        List<ShelfStockResponseDto> result =
                shelfStockService.getShelfStockByShelfId(20L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(20L, result.getFirst().shelfId());
    }

    @Test
    void getShelfStockByProductIdShouldReturnStock() {
        // Arrange
        when(shelfStockRepository.findByProductId(10L))
                .thenReturn(List.of(shelfStock));

        // Act
        List<ShelfStockResponseDto> result =
                shelfStockService.getShelfStockByProductId(10L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(10L, result.getFirst().productId());
    }

    @Test
    void getShelfStockByExpirationDateShouldReturnStock() {
        // Arrange
        when(shelfStockRepository.findByExpirationDate(expirationDate))
                .thenReturn(List.of(shelfStock));

        // Act
        List<ShelfStockResponseDto> result =
                shelfStockService
                        .getShelfStockByExpirationDate(expirationDate);

        // Assert
        assertEquals(1, result.size());
        assertEquals(
                expirationDate,
                result.getFirst().expirationDate()
        );
    }

    @Test
    void getExpiredShelfStockShouldReturnExpiredStock() {
        // Arrange
        ShelfStock expired = new ShelfStock();
        expired.setId(41L);
        expired.setShelf(shelf);
        expired.setProduct(product);
        expired.setQuantity(5);
        expired.setExpirationDate(
                LocalDate.now().minusDays(1)
        );
        expired.setStatus(StockStatus.EXPIRED);

        when(shelfStockRepository.findByExpirationDateBefore(
                LocalDate.now()))
                .thenReturn(List.of(expired));

        // Act
        List<ShelfStockResponseDto> result =
                shelfStockService.getExpiredShelfStock();

        // Assert
        assertEquals(1, result.size());
        assertEquals(41L, result.getFirst().id());
    }

    @Test
    void updateShelfStockShouldUpdateQuantityAndExpirationDate() {
        // Arrange
        LocalDate newExpiration =
                LocalDate.now().plusDays(20);

        ShelfStockRequestDto dto =
                new ShelfStockRequestDto(
                        20L,
                        10L,
                        50,
                        newExpiration
                );

        when(shelfStockRepository.findById(40L))
                .thenReturn(Optional.of(shelfStock));

        // Act
        ShelfStockResponseDto result =
                shelfStockService.updateShelfStock(40L, dto);

        // Assert
        assertEquals(50, result.quantity());
        assertEquals(
                newExpiration,
                result.expirationDate()
        );

        assertEquals(50, shelfStock.getQuantity());
        assertEquals(
                newExpiration,
                shelfStock.getExpirationDate()
        );
    }

    @Test
    void deleteShelfStockShouldDeleteStock() {
        // Arrange
        when(shelfStockRepository.findById(40L))
                .thenReturn(Optional.of(shelfStock));

        // Act
        shelfStockService.deleteShelfStock(40L);

        // Assert
        verify(shelfStockRepository)
                .delete(shelfStock);
    }
}