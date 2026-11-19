package com.skr1l.backend.shelfStock.service;

import com.skr1l.backend.exception.productExceptions.ProductIsNotFoundException;
import com.skr1l.backend.exception.shelfExceptions.ShelfIsNotFoundException;
import com.skr1l.backend.exception.shelfStockExceptions.ShelfStockIsNotFoundException;
import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.shelfStock.enums.StockStatus;
import com.skr1l.backend.product.Product;
import com.skr1l.backend.product.repository.ProductRepository;
import com.skr1l.backend.shelf.Shelf;
import com.skr1l.backend.shelf.repository.ShelfRepository;
import com.skr1l.backend.shelfStock.ShelfStock;
import com.skr1l.backend.shelfStock.dto.ShelfStockRequestDto;
import com.skr1l.backend.shelfStock.dto.ShelfStockResponseDto;
import com.skr1l.backend.shelfStock.repository.ShelfStockRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ShelfStockServiceImpl implements ShelfStockService {

    private static final AppLogger log = LoggerFactory.getLogger(ShelfStockServiceImpl.class);

    private final ShelfStockRepository shelfStockRepository;

    private final ShelfRepository shelfRepository;

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public Long createShelfStock(ShelfStockRequestDto dto) {
        log.info("createShelfStock");

        Objects.requireNonNull(dto, "dto is null");

        Shelf shelf = getShelfEntityById(dto.shelfId());
        Product product = getProductEntityById(dto.productId());

        ShelfStock shelfStock = new ShelfStock();

        shelfStock.setShelf(shelf);
        shelfStock.setProduct(product);
        shelfStock.setQuantity(dto.quantity());
        shelfStock.setExpirationDate(dto.expirationDate());
        shelfStock.setStatus(StockStatus.ACTIVE);

        shelfStockRepository.save(shelfStock);

        return shelfStock.getId();
    }

    @Override
    public ShelfStockResponseDto getShelfStockById(Long id) {
        log.info("getShelfStockById");

        Objects.requireNonNull(id, "id is null");

        return toResponseDTO(getShelfStockEntityById(id));
    }

    @Override
    public List<ShelfStockResponseDto> getAllShelfStock() {
        log.info("getAllShelfStock");

        return shelfStockRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    public List<ShelfStockResponseDto> getShelfStockByShelfId(Long shelfId) {
        log.info("getShelfStockByShelfId");

        Objects.requireNonNull(shelfId, "shelfId is null");

        return shelfStockRepository.findByShelfId(shelfId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    public List<ShelfStockResponseDto> getShelfStockByProductId(Long productId) {
        log.info("getShelfStockByProductId");

        Objects.requireNonNull(productId, "productId is null");

        return shelfStockRepository.findByProductId(productId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public ShelfStockResponseDto updateShelfStock(Long id, ShelfStockRequestDto dto) {
        log.info("updateShelfStock");

        Objects.requireNonNull(id, "id is null");
        Objects.requireNonNull(dto, "dto is null");

        ShelfStock shelfStock = getShelfStockEntityById(id);

        shelfStock.setQuantity(dto.quantity());
        shelfStock.setExpirationDate(dto.expirationDate());

        return toResponseDTO(shelfStock);
    }

    @Override
    @Transactional
    public void deleteShelfStock(Long id) {
        log.info("deleteShelfStock");

        Objects.requireNonNull(id, "id is null");

        ShelfStock shelfStock = getShelfStockEntityById(id);

        shelfStockRepository.delete(shelfStock);
    }

    private ShelfStock getShelfStockEntityById(Long id) {
        return shelfStockRepository.findById(id)
                .orElseThrow(() -> new ShelfStockIsNotFoundException("Shelf stock is not found"));
    }

    private Shelf getShelfEntityById(Long id) {
        return shelfRepository.findById(id)
                .orElseThrow(() -> new ShelfIsNotFoundException("Shelf is not found"));
    }

    private Product getProductEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductIsNotFoundException("Product is not found"));
    }

    private ShelfStockResponseDto toResponseDTO(ShelfStock shelfStock) {
        return new ShelfStockResponseDto(
                shelfStock.getId(),
                shelfStock.getShelf().getId(),
                shelfStock.getProduct().getId(),
                shelfStock.getQuantity(),
                shelfStock.getExpirationDate(),
                shelfStock.getStatus());
    }

    @Override
    public List<ShelfStockResponseDto> getExpiredShelfStock() {
        log.info("getExpiredShelfStock");

        return shelfStockRepository
                .findByExpirationDateBefore(LocalDate.now())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    public List<ShelfStockResponseDto> getShelfStockByExpirationDate(
            LocalDate expirationDate
    ) {
        log.info("getShelfStockByExpirationDate");

        Objects.requireNonNull(expirationDate, "expirationDate is null");

        return shelfStockRepository
                .findByExpirationDate(expirationDate)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }
}