package com.skr1l.backend.shelf.controller;

import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.shelf.dto.ShelfRequestDto;
import com.skr1l.backend.shelf.dto.ShelfResponseDto;
import com.skr1l.backend.shelf.service.ShelfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/shelves")
public class ShelfController {

    private static final AppLogger log = LoggerFactory.getLogger(ShelfController.class);

    private final ShelfService shelfService;

    @GetMapping("/{id}")
    public ResponseEntity<ShelfResponseDto> getShelfById(@PathVariable Long id) {
        log.info("getShelfById controller");

        return ResponseEntity.ok(shelfService.getShelfById(id));
    }

    @GetMapping
    public ResponseEntity<List<ShelfResponseDto>> getAllShelves() {
        log.info("getAllShelves controller");

        return ResponseEntity.ok(shelfService.getAllShelves());
    }

    @PostMapping
    public ResponseEntity<Long> createShelf(@Valid @RequestBody ShelfRequestDto dto) {
        log.info("createShelf controller");

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(shelfService.createShelf(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShelfResponseDto> updateShelf(
            @PathVariable Long id, @Valid @RequestBody ShelfRequestDto dto) {
        log.info("updateShelf controller");

        return ResponseEntity.ok(shelfService.updateShelf(id, dto));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateShelf(@PathVariable Long id) {
        log.info("activateShelf controller");

        shelfService.activateShelf(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateShelf(@PathVariable Long id) {
        log.info("deactivateShelf controller");

        shelfService.deactivateShelf(id);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShelf(@PathVariable Long id) {
        log.info("deleteShelf controller");

        shelfService.deleteShelfById(id);

        return ResponseEntity.noContent().build();
    }
}