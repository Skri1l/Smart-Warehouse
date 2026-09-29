package com.skr1l.backend.rack.controller;

import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.rack.dto.RackRequestDto;
import com.skr1l.backend.rack.dto.RackResponseDto;
import com.skr1l.backend.rack.service.RackService;
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
@RequestMapping("/racks")
public class RackController {

    private final static AppLogger log = LoggerFactory.getLogger(RackController.class);

    private final RackService rackService;

    @GetMapping("/{id}")
    public ResponseEntity<RackResponseDto> getRack(@PathVariable Long id) {
        log.info("getRack controller");
        return ResponseEntity.ok(rackService.getRackById(id));
    }

    @GetMapping
    public ResponseEntity<List<RackResponseDto>> getAllRacks() {
        log.info("getAllRacks controller");
        return ResponseEntity.ok(rackService.getAllRacks());
    }

    @PostMapping("/warehouse/{warehouseId}")
    public ResponseEntity<Long> createRack(@PathVariable Long warehouseId,
                                           @Valid @RequestBody RackRequestDto dto) {
        log.info("createRack controller");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rackService.createRack(dto, warehouseId));
    }

    @PutMapping("/{rackId}")
    public ResponseEntity<RackResponseDto> updateRack(
            @Valid @RequestBody RackRequestDto dto, @PathVariable Long rackId){
        log.info("updateRack controller");

        return ResponseEntity.ok(rackService.updateRack(dto, rackId, dto.warehouseId()));
    }

    @DeleteMapping("/{rackId}")
    public ResponseEntity<Void> deleteRack(@PathVariable Long rackId) {
        log.info("deleteRack controller");

        rackService.deleteRackById(rackId);

        return ResponseEntity.noContent().build();
    }
}
