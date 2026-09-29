package com.skr1l.backend.device.controller;

import com.skr1l.backend.device.dto.DeviceRequestDto;
import com.skr1l.backend.device.dto.DeviceResponseDto;
import com.skr1l.backend.device.service.DeviceService;
import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/devices")
public class DeviceController {

    private static final AppLogger log = LoggerFactory.getLogger(DeviceController.class);

    private final DeviceService deviceService;

    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponseDto> getDeviceById(@PathVariable Long id) {
        log.info("getDeviceById controller");

        return ResponseEntity.ok(deviceService.getDeviceById(id));
    }

    @GetMapping
    public ResponseEntity<List<DeviceResponseDto>> getAllDevices() {
        log.info("getAllDevices controller");

        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @PostMapping
    public ResponseEntity<Long> createDevice(@Valid @RequestBody DeviceRequestDto dto) {
        log.info("createDevice controller");

        return ResponseEntity.status(HttpStatus.CREATED).body(deviceService.createDevice(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceResponseDto> updateDevice(
            @PathVariable Long id, @Valid @RequestBody DeviceRequestDto dto) {
        log.info("updateDevice controller");

        return ResponseEntity.ok(deviceService.updateDevice(id, dto));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateDevice(@PathVariable Long id) {
        log.info("activateDevice controller");

        deviceService.activateDevice(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateDevice(@PathVariable Long id) {
        log.info("deactivateDevice controller");

        deviceService.deactivateDevice(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/heartbeat")
    public ResponseEntity<Void> heartbeat(@PathVariable Long id) {
        log.info("heartbeat controller");

        deviceService.heartbeat(id);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        log.info("deleteDevice controller");

        deviceService.deleteDeviceById(id);

        return ResponseEntity.noContent().build();
    }
}