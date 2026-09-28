package com.skr1l.backend.warehouse.controller;

import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.warehouse.dto.WarehouseRequestDTO;
import com.skr1l.backend.warehouse.dto.WarehouseResponseDTO;
import com.skr1l.backend.warehouse.service.WarehouseService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/warehouses")
public class WarehouseController {

    private final static AppLogger log = LoggerFactory.getLogger(WarehouseController.class);

    private final WarehouseService warehouseService;

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseResponseDTO> getWarehouseById(@PathVariable Long id) {
        log.info("getWarehouseById");
        return ResponseEntity.ok(warehouseService.getWarehouseById(id));
    }

    @GetMapping
    public ResponseEntity<List<WarehouseResponseDTO>> getAllWarehouses() {
        log.info("WarehouseController.getAllWarehouses");
        return ResponseEntity.ok(warehouseService.getAllWarehouses());
    }

    @PostMapping
    public ResponseEntity<Long> createWarehouse(@Valid @RequestBody WarehouseRequestDTO dto) {
        log.info("createWarehouse");

        Long id = warehouseService.createWarehouse(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WarehouseResponseDTO> updateWarehouse(
            @PathVariable Long id, @Valid @RequestBody WarehouseRequestDTO dto) {
        log.info("updateWarehouse");
        return ResponseEntity.ok(warehouseService.updateWarehouse(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWarehouse(@PathVariable Long id) {
        log.info("deleteWarehouse");

        warehouseService.deleteWarehouse(id);

        return ResponseEntity.notFound().build();
    }
}
