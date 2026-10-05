package com.skr1l.backend.stockMovement.service;

import com.skr1l.backend.exception.productExceptions.ProductIsNotFoundException;
import com.skr1l.backend.exception.shelfExceptions.ShelfIsNotFoundException;
import com.skr1l.backend.exception.stockMovement.StockMovementIsNotFoundException;
import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.product.Product;
import com.skr1l.backend.product.repository.ProductRepository;
import com.skr1l.backend.shelf.Shelf;
import com.skr1l.backend.shelf.repository.ShelfRepository;
import com.skr1l.backend.stockMovement.StockMovement;
import com.skr1l.backend.stockMovement.dto.StockMovementRequestDto;
import com.skr1l.backend.stockMovement.dto.StockMovementResponseDto;
import com.skr1l.backend.stockMovement.enums.StockMovementSource;
import com.skr1l.backend.stockMovement.enums.StockMovementType;
import com.skr1l.backend.stockMovement.repository.StockMovementRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StockMovementServiceImpl implements StockMovementService {

    private static final AppLogger log = LoggerFactory.getLogger(StockMovementServiceImpl.class);

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final ShelfRepository shelfRepository;

    @Override
    @Transactional
    public Long createStockMovement(StockMovementRequestDto dto) {
        log.info("createStockMovement");

        Objects.requireNonNull(dto, "dto is null");

        Product product = getProductEntityById(dto.productId());

        Shelf shelf = getShelfEntityById(dto.shelfId());

        StockMovement movement = new StockMovement();

        movement.setProduct(product);
        movement.setShelf(shelf);
        movement.setQuantityDelta(dto.quantityDelta());
        movement.setType(dto.type());
        movement.setSource(dto.source());
        movement.setCreatedAt(LocalDateTime.now());

        stockMovementRepository.save(movement);

        return movement.getId();
    }

    @Override
    public StockMovementResponseDto getStockMovementById(Long id) {
        log.info("getStockMovementById");

        Objects.requireNonNull(id, "id is null");

        return toResponseDto(getStockMovementEntityById(id));
    }

    @Override
    public List<StockMovementResponseDto> getAllStockMovements() {
        log.info("getAllStockMovements");

        return stockMovementRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<StockMovementResponseDto>
    getStockMovementsByProductId(Long productId) {
        log.info("getStockMovementsByProductId");

        Objects.requireNonNull(productId, "productId is null");

        return stockMovementRepository
                .findByProductId(productId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<StockMovementResponseDto>
    getStockMovementsByShelfId(Long shelfId) {
        log.info("getStockMovementsByShelfId");

        Objects.requireNonNull(shelfId, "shelfId is null");

        return stockMovementRepository
                .findByShelfId(shelfId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<StockMovementResponseDto>
    getStockMovementsByType(StockMovementType type) {
        log.info("getStockMovementsByType");

        Objects.requireNonNull(type, "type is null");

        return stockMovementRepository
                .findByType(type)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<StockMovementResponseDto>
    getStockMovementsBySource(StockMovementSource source) {
        log.info("getStockMovementsBySource");

        Objects.requireNonNull(source, "source is null");

        return stockMovementRepository
                .findBySource(source)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<StockMovementResponseDto>
    getStockMovementsByDateRange(LocalDateTime from, LocalDateTime to) {
        log.info("getStockMovementsByDateRange");

        Objects.requireNonNull(from, "from is null");
        Objects.requireNonNull(to, "to is null");

        return stockMovementRepository
                .findByCreatedAtBetween(from, to)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteStockMovement(Long id) {
        log.info("deleteStockMovement");

        Objects.requireNonNull(id, "id is null");

        StockMovement movement = getStockMovementEntityById(id);

        stockMovementRepository.delete(movement);
    }

    private StockMovement getStockMovementEntityById(Long id) {
        return stockMovementRepository.findById(id)
                .orElseThrow(() -> new StockMovementIsNotFoundException(
                        "Stock movement with id " + id + " is not found"));
    }

    private Product getProductEntityById(Long id) {
        return productRepository.findById(id).orElseThrow(() ->
                new ProductIsNotFoundException("Product with id " + id + " is not found"));
    }

    private Shelf getShelfEntityById(Long id) {
        return shelfRepository.findById(id).orElseThrow(() ->
                new ShelfIsNotFoundException("Shelf with id " + id + " is not found"));
    }

    private StockMovementResponseDto toResponseDto(StockMovement movement) {

        return new StockMovementResponseDto(
                movement.getId(),
                movement.getProduct().getId(),
                movement.getShelf().getId(),
                movement.getQuantityDelta(),
                movement.getType(),
                movement.getSource(),
                movement.getCreatedAt());
    }
}