package com.skr1l.backend.warehouse.controller;

import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.warehouse.dto.WarehouseRequestDTO;
import com.skr1l.backend.warehouse.dto.WarehouseResponseDTO;
import com.skr1l.backend.warehouse.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Tag(name = "Warehouses", description = "Warehouse management API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/warehouses")
public class WarehouseController {

    private final static AppLogger log = LoggerFactory.getLogger(WarehouseController.class);

    private final WarehouseService warehouseService;

    @Operation(summary = "Get warehouse by id")
    @ApiResponse(responseCode = "200", description = "Warehouse found")
    @ApiResponse(responseCode = "404", description = "Warehouse not found")
    @GetMapping("/{id}")
    public ResponseEntity<WarehouseResponseDTO> getWarehouseById(@PathVariable Long id) {
        log.info("getWarehouseById");
        return ResponseEntity.ok(warehouseService.getWarehouseById(id));
    }

    @Operation(summary = "Get all warehouses")
    @ApiResponse(responseCode = "200", description = "Warehouses returned successfully")
    @GetMapping
    public ResponseEntity<List<WarehouseResponseDTO>> getAllWarehouses() {
        log.info("WarehouseController.getAllWarehouses");
        return ResponseEntity.ok(warehouseService.getAllWarehouses());
    }

    @Operation(summary = "Create a warehouse")
    @ApiResponse(responseCode = "201", description = "Warehouse created")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "409", description = "Warehouse already exists")
    @PostMapping
    public ResponseEntity<Long> createWarehouse(@Valid @RequestBody WarehouseRequestDTO dto) {
        log.info("createWarehouse");

        Long id = warehouseService.createWarehouse(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    @Operation(summary = "Update a warehouse")
    @ApiResponse(responseCode = "200", description = "Warehouse updated")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "404", description = "Warehouse not found")
    @ApiResponse(responseCode = "409", description = "Warehouse already exists")
    @PutMapping("/{id}")
    public ResponseEntity<WarehouseResponseDTO> updateWarehouse(
            @PathVariable Long id, @Valid @RequestBody WarehouseRequestDTO dto) {
        log.info("updateWarehouse");
        return ResponseEntity.ok(warehouseService.updateWarehouse(id, dto));
    }

    @Operation(summary = "Delete a warehouse")
    @ApiResponse(responseCode = "204", description = "Warehouse deleted")
    @ApiResponse(responseCode = "404", description = "Warehouse not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWarehouse(@PathVariable Long id) {
        log.info("deleteWarehouse");

        warehouseService.deleteWarehouse(id);

        return ResponseEntity.noContent().build();
    }
}
