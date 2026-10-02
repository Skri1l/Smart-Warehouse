package com.skr1l.backend.imageEvent.controller;

import com.skr1l.backend.imageEvent.dto.ImageEventRequestDto;
import com.skr1l.backend.imageEvent.dto.ImageEventResponseDto;
import com.skr1l.backend.imageEvent.enums.ImageProcessingStatus;
import com.skr1l.backend.imageEvent.service.ImageEventService;
import com.skr1l.backend.logging.AppLogger;
import com.skr1l.backend.logging.LoggerFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/image-events")
public class ImageEventController {

    private static final AppLogger log = LoggerFactory.getLogger(ImageEventController.class);

    private final ImageEventService imageEventService;

    @PostMapping
    public ResponseEntity<Long> createImageEvent(@Valid @RequestBody ImageEventRequestDto dto) {

        log.info("createImageEvent controller");

        return ResponseEntity.status(HttpStatus.CREATED).body(imageEventService.createImageEvent(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImageEventResponseDto> getImageEventById(@PathVariable Long id) {

        return ResponseEntity.ok(imageEventService.getImageEventById(id));
    }

    @GetMapping
    public ResponseEntity<List<ImageEventResponseDto>> getAllImageEvents() {

        return ResponseEntity.ok(imageEventService.getAllImageEvents());
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<ImageEventResponseDto>> getImageEventsByDeviceId(
            @PathVariable Long deviceId) {

        return ResponseEntity.ok(imageEventService.getImageEventsByDeviceId(deviceId));
    }

    @GetMapping("/shelf/{shelfId}")
    public ResponseEntity<List<ImageEventResponseDto>> getImageEventsByShelfId(
            @PathVariable Long shelfId) {

        return ResponseEntity.ok(imageEventService.getImageEventsByShelfId(shelfId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ImageEventResponseDto>> getImageEventsByStatus(
            @PathVariable ImageProcessingStatus status) {

        return ResponseEntity.ok(imageEventService.getImageEventsByStatus(status));
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<ImageEventResponseDto>> getImageEventsByDateRange(
            @RequestParam LocalDateTime from, @RequestParam LocalDateTime to) {

        return ResponseEntity.ok(imageEventService.getImageEventsByDateRange(from, to));
    }

    @PatchMapping("/{id}/processing")
    public ResponseEntity<Void> markAsProcessing(@PathVariable Long id) {
        imageEventService.markAsProcessing(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/processed")
    public ResponseEntity<Void> markAsProcessed(@PathVariable Long id) {
        imageEventService.markAsProcessed(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/failed")
    public ResponseEntity<Void> markAsFailed(@PathVariable Long id) {
        imageEventService.markAsFailed(id);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImageEvent(@PathVariable Long id) {
        imageEventService.deleteImageEvent(id);

        return ResponseEntity.noContent().build();
    }
}