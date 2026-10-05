package com.skr1l.backend.rack.controller;

import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import com.skr1l.backend.rack.dto.RackRequestDto;
import com.skr1l.backend.rack.dto.RackResponseDto;
import com.skr1l.backend.rack.service.RackService;
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

@Tag(name = "Racks", description = "Rack management API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/racks")
public class RackController {

    private final static AppLogger log = LoggerFactory.getLogger(RackController.class);

    private final RackService rackService;

    @Operation(summary = "Get rack by id")
    @ApiResponse(responseCode = "200", description = "Rack found")
    @ApiResponse(responseCode = "404", description = "Rack not found")
    @GetMapping("/{id}")
    public ResponseEntity<RackResponseDto> getRack(@PathVariable Long id) {
        log.info("getRack controller");
        return ResponseEntity.ok(rackService.getRackById(id));
    }

    @Operation(summary = "Get all racks")
    @ApiResponse(responseCode = "200", description = "Racks returned successfully")
    @GetMapping
    public ResponseEntity<List<RackResponseDto>> getAllRacks() {
        log.info("getAllRacks controller");
        return ResponseEntity.ok(rackService.getAllRacks());
    }

    @Operation(summary = "Create a rack")
    @ApiResponse(responseCode = "201", description = "Rack created")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "404", description = "Warehouse not found")
    @PostMapping("/warehouse/{warehouseId}")
    public ResponseEntity<Long> createRack(@PathVariable Long warehouseId,
                                           @Valid @RequestBody RackRequestDto dto) {
        log.info("createRack controller");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(rackService.createRack(dto, warehouseId));
    }

    @Operation(summary = "Update a rack")
    @ApiResponse(responseCode = "200", description = "Rack updated")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "404", description = "Rack or warehouse not found")
    @PutMapping("/{rackId}")
    public ResponseEntity<RackResponseDto> updateRack(
            @Valid @RequestBody RackRequestDto dto, @PathVariable Long rackId){
        log.info("updateRack controller");

        return ResponseEntity.ok(rackService.updateRack(dto, rackId, dto.warehouseId()));
    }

    @Operation(summary = "Delete a rack")
    @ApiResponse(responseCode = "204", description = "Rack deleted")
    @ApiResponse(responseCode = "404", description = "Rack not found")
    @DeleteMapping("/{rackId}")
    public ResponseEntity<Void> deleteRack(@PathVariable Long rackId) {
        log.info("deleteRack controller");

        rackService.deleteRackById(rackId);

        return ResponseEntity.noContent().build();
    }
}
