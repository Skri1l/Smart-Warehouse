package com.skr1l.backend.product.service;

import com.skr1l.backend.exception.productExceptions.ProductAlreadyExistsException;
import com.skr1l.backend.exception.productExceptions.ProductIsNotFoundException;
import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.product.Product;
import com.skr1l.backend.product.dto.ProductRequestDto;
import com.skr1l.backend.product.dto.ProductResponseDto;
import com.skr1l.backend.product.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final AppLogger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public Long createProduct(ProductRequestDto dto) {
        log.info("createProduct");

        Objects.requireNonNull(dto, "dto is null");

        if (productRepository.existsBySku(dto.sku())) {
            throw new ProductAlreadyExistsException("Product with this SKU already exists");
        }

        Product product = new Product();
        product.setSku(dto.sku());
        product.setName(dto.name());
        product.setDescription(dto.description());

        productRepository.save(product);

        return product.getId();
    }

    @Override
    public ProductResponseDto getProductById(Long id) {
        log.info("getProductById");

        Objects.requireNonNull(id, "id is null");

        return toResponseDTO(getProductEntityById(id));
    }

    @Override
    public ProductResponseDto getProductBySku(String sku) {
        log.info("getProductBySku");

        Objects.requireNonNull(sku, "sku is null");

        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ProductIsNotFoundException(
                        "Product with SKU " + sku + " is not found"));

        return toResponseDTO(product);
    }

    @Override
    public List<ProductResponseDto> getProductsByName(String name) {
        log.info("getProductsByName");

        Objects.requireNonNull(name, "name is null");

        return productRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    public List<ProductResponseDto> getAllProducts() {
        log.info("getAllProducts");

        return productRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public ProductResponseDto updateProduct(Long id, ProductRequestDto dto) {
        log.info("updateProduct");

        Objects.requireNonNull(id, "id is null");
        Objects.requireNonNull(dto, "dto is null");

        Product product = getProductEntityById(id);

        if (productRepository.existsBySku(dto.sku()) && !product.getSku().equals(dto.sku())) {
            throw new ProductAlreadyExistsException("Product with this SKU already exists");
        }

        product.setSku(dto.sku());
        product.setName(dto.name());
        product.setDescription(dto.description());

        return toResponseDTO(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        log.info("deleteProduct");

        Objects.requireNonNull(id, "id is null");

        Product product = getProductEntityById(id);

        productRepository.delete(product);
    }

    private Product getProductEntityById(Long id) {
        log.info("getProductEntityById");

        return productRepository.findById(id)
                .orElseThrow(() -> new ProductIsNotFoundException("Product is not found"));
    }

    private ProductResponseDto toResponseDTO(Product product) {
        log.info("productToResponseDTO");

        return new ProductResponseDto(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription());
    }
}