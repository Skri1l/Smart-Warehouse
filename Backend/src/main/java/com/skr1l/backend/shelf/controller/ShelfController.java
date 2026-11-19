package com.skr1l.backend.shelf.controller;

import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.shelf.dto.ShelfRequestDto;
import com.skr1l.backend.shelf.dto.ShelfResponseDto;
import com.skr1l.backend.shelf.service.ShelfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Shelves", description = "Shelf management API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/shelves")
public class ShelfController {

    private static final AppLogger log = LoggerFactory.getLogger(ShelfController.class);

    private final ShelfService shelfService;

    @Operation(summary = "Get shelf by id")
    @ApiResponse(responseCode = "200", description = "Shelf found")
    @ApiResponse(responseCode = "404", description = "Shelf not found")
    @GetMapping("/{id}")
    public ResponseEntity<ShelfResponseDto> getShelfById(@PathVariable Long id) {
        log.info("getShelfById controller");

        return ResponseEntity.ok(shelfService.getShelfById(id));
    }

    @Operation(summary = "Get all shelves")
    @ApiResponse(responseCode = "200", description = "Shelves returned successfully")
    @GetMapping
    public ResponseEntity<List<ShelfResponseDto>> getAllShelves() {
        log.info("getAllShelves controller");

        return ResponseEntity.ok(shelfService.getAllShelves());
    }

    @Operation(summary = "Create a shelf")
    @ApiResponse(responseCode = "201", description = "Shelf created")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "404", description = "Rack not found")
    @PostMapping
    public ResponseEntity<Long> createShelf(@Valid @RequestBody ShelfRequestDto dto) {
        log.info("createShelf controller");

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(shelfService.createShelf(dto));
    }

    @Operation(summary = "Update a shelf")
    @ApiResponse(responseCode = "200", description = "Shelf updated")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "404", description = "Shelf or rack not found")
    @PutMapping("/{id}")
    public ResponseEntity<ShelfResponseDto> updateShelf(
            @PathVariable Long id, @Valid @RequestBody ShelfRequestDto dto) {
        log.info("updateShelf controller");

        return ResponseEntity.ok(shelfService.updateShelf(id, dto));
    }

    @Operation(summary = "Activate a shelf")
    @ApiResponse(responseCode = "204", description = "Shelf activated")
    @ApiResponse(responseCode = "404", description = "Shelf not found")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateShelf(@PathVariable Long id) {
        log.info("activateShelf controller");

        shelfService.activateShelf(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Deactivate a shelf")
    @ApiResponse(responseCode = "204", description = "Shelf deactivated")
    @ApiResponse(responseCode = "404", description = "Shelf not found")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateShelf(@PathVariable Long id) {
        log.info("deactivateShelf controller");

        shelfService.deactivateShelf(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete a shelf")
    @ApiResponse(responseCode = "204", description = "Shelf deleted")
    @ApiResponse(responseCode = "404", description = "Shelf not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShelf(@PathVariable Long id) {
        log.info("deleteShelf controller");

        shelfService.deleteShelfById(id);

        return ResponseEntity.noContent().build();
    }
}