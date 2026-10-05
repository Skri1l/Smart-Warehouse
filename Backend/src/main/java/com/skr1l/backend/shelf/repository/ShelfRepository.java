package com.skr1l.backend.shelf.repository;

import com.skr1l.backend.shelf.Shelf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShelfRepository extends JpaRepository<Shelf, Long> {
}
