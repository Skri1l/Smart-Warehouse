package com.skr1l.backend.device.controller;

import com.skr1l.backend.device.dto.DeviceRequestDto;
import com.skr1l.backend.device.dto.DeviceResponseDto;
import com.skr1l.backend.device.service.DeviceService;
import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Devices", description = "Device management API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/devices")
public class DeviceController {

    private static final AppLogger log = LoggerFactory.getLogger(DeviceController.class);

    private final DeviceService deviceService;

    @Operation(summary = "Get device by id")
    @ApiResponse(responseCode = "200", description = "Device found")
    @ApiResponse(responseCode = "404", description = "Device not found")
    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponseDto> getDeviceById(@PathVariable Long id) {
        log.info("getDeviceById controller");

        return ResponseEntity.ok(deviceService.getDeviceById(id));
    }

    @Operation(summary = "Get all devices")
    @ApiResponse(responseCode = "200", description = "Devices returned successfully")
    @GetMapping
    public ResponseEntity<List<DeviceResponseDto>> getAllDevices() {
        log.info("getAllDevices controller");

        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @Operation(summary = "Create a device")
    @ApiResponse(responseCode = "201", description = "Device created")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "404", description = "Shelf not found")
    @ApiResponse(responseCode = "409", description = "Device already exists or shelf already has a device")
    @PostMapping
    public ResponseEntity<Long> createDevice(@Valid @RequestBody DeviceRequestDto dto) {
        log.info("createDevice controller");

        return ResponseEntity.status(HttpStatus.CREATED).body(deviceService.createDevice(dto));
    }

    @Operation(summary = "Update a device")
    @ApiResponse(responseCode = "200", description = "Device updated")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "404", description = "Device or shelf not found")
    @ApiResponse(responseCode = "409", description = "Device already exists or shelf already has a device")
    @PutMapping("/{id}")
    public ResponseEntity<DeviceResponseDto> updateDevice(@PathVariable Long id, @Valid @RequestBody DeviceRequestDto dto) {
        log.info("updateDevice controller");

        return ResponseEntity.ok(deviceService.updateDevice(id, dto));
    }

    @Operation(summary = "Activate a device")
    @ApiResponse(responseCode = "204", description = "Device activated")
    @ApiResponse(responseCode = "404", description = "Device not found")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateDevice(@PathVariable Long id) {
        log.info("activateDevice controller");

        deviceService.activateDevice(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Deactivate a device")
    @ApiResponse(responseCode = "204", description = "Device deactivated")
    @ApiResponse(responseCode = "404", description = "Device not found")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateDevice(@PathVariable Long id) {
        log.info("deactivateDevice controller");

        deviceService.deactivateDevice(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Register device heartbeat")
    @ApiResponse(responseCode = "204", description = "Heartbeat registered")
    @ApiResponse(responseCode = "404", description = "Device not found")
    @PatchMapping("/{id}/heartbeat")
    public ResponseEntity<Void> heartbeat(@PathVariable Long id) {
        log.info("heartbeat controller");

        deviceService.heartbeat(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete a device")
    @ApiResponse(responseCode = "204", description = "Device deleted")
    @ApiResponse(responseCode = "404", description = "Device not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        log.info("deleteDevice controller");

        deviceService.deleteDeviceById(id);

        return ResponseEntity.noContent().build();
    }
}