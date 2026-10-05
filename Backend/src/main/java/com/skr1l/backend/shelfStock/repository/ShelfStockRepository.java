package com.skr1l.backend.shelfStock.repository;

import com.skr1l.backend.shelfStock.ShelfStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ShelfStockRepository extends JpaRepository<ShelfStock, Long> {

    List<ShelfStock> findByShelfId(Long shelfId);

    List<ShelfStock> findByProductId(Long productId);

    List<ShelfStock> findByExpirationDate(LocalDate expirationDate);

    List<ShelfStock> findByExpirationDateBefore(LocalDate date);
}