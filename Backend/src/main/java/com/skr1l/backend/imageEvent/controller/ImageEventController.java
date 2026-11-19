package com.skr1l.backend.imageEvent.controller;

import com.skr1l.backend.imageEvent.dto.ImageEventRequestDto;
import com.skr1l.backend.imageEvent.dto.ImageEventResponseDto;
import com.skr1l.backend.imageEvent.enums.ImageProcessingStatus;
import com.skr1l.backend.imageEvent.service.ImageEventService;
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
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Image Events", description = "Image event management API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/image-events")
public class ImageEventController {

    private static final AppLogger log = LoggerFactory.getLogger(ImageEventController.class);

    private final ImageEventService imageEventService;

    @Operation(summary = "Create an image event")
    @ApiResponse(responseCode = "201", description = "Image event created")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "404", description = "Device not found")
    @PostMapping
    public ResponseEntity<Long> createImageEvent(@Valid @RequestBody ImageEventRequestDto dto) {
        log.info("createImageEvent controller");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(imageEventService.createImageEvent(dto));
    }

    @Operation(summary = "Get image event by id")
    @ApiResponse(responseCode = "200", description = "Image event found")
    @ApiResponse(responseCode = "404", description = "Image event not found")
    @GetMapping("/{id}")
    public ResponseEntity<ImageEventResponseDto> getImageEventById(@PathVariable Long id) {

        return ResponseEntity.ok(imageEventService.getImageEventById(id));
    }

    @Operation(summary = "Get all image events")
    @ApiResponse(responseCode = "200", description = "Image events returned successfully")
    @GetMapping
    public ResponseEntity<List<ImageEventResponseDto>> getAllImageEvents() {

        return ResponseEntity.ok(imageEventService.getAllImageEvents());
    }

    @Operation(summary = "Get image events by device id")
    @ApiResponse(responseCode = "200", description = "Image events returned successfully")
    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<ImageEventResponseDto>> getImageEventsByDeviceId(
            @PathVariable Long deviceId) {

        return ResponseEntity.ok(imageEventService.getImageEventsByDeviceId(deviceId));
    }

    @Operation(summary = "Get image events by shelf id")
    @ApiResponse(responseCode = "200", description = "Image events returned successfully")
    @GetMapping("/shelf/{shelfId}")
    public ResponseEntity<List<ImageEventResponseDto>> getImageEventsByShelfId(
            @PathVariable Long shelfId) {

        return ResponseEntity.ok(imageEventService.getImageEventsByShelfId(shelfId));
    }

    @Operation(summary = "Get image events by processing status")
    @ApiResponse(responseCode = "200", description = "Image events returned successfully")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ImageEventResponseDto>> getImageEventsByStatus(
            @PathVariable ImageProcessingStatus status) {

        return ResponseEntity.ok(imageEventService.getImageEventsByStatus(status));
    }

    @Operation(summary = "Get image events by date range")
    @ApiResponse(responseCode = "200", description = "Image events returned successfully")
    @GetMapping("/date-range")
    public ResponseEntity<List<ImageEventResponseDto>> getImageEventsByDateRange(
            @RequestParam LocalDateTime from, @RequestParam LocalDateTime to) {

        return ResponseEntity.ok(imageEventService.getImageEventsByDateRange(from, to));
    }

    @Operation(summary = "Mark image event as processing")
    @ApiResponse(responseCode = "204", description = "Image event marked as processing")
    @ApiResponse(responseCode = "404", description = "Image event not found")
    @PatchMapping("/{id}/processing")
    public ResponseEntity<Void> markAsProcessing(@PathVariable Long id) {
        imageEventService.markAsProcessing(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark image event as processed")
    @ApiResponse(responseCode = "204", description = "Image event marked as processed")
    @ApiResponse(responseCode = "404", description = "Image event not found")
    @PatchMapping("/{id}/processed")
    public ResponseEntity<Void> markAsProcessed(@PathVariable Long id) {
        imageEventService.markAsProcessed(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark image event as failed")
    @ApiResponse(responseCode = "204", description = "Image event marked as failed")
    @ApiResponse(responseCode = "404", description = "Image event not found")
    @PatchMapping("/{id}/failed")
    public ResponseEntity<Void> markAsFailed(@PathVariable Long id) {
        imageEventService.markAsFailed(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete an image event")
    @ApiResponse(responseCode = "204", description = "Image event deleted")
    @ApiResponse(responseCode = "404", description = "Image event not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImageEvent(@PathVariable Long id) {
        imageEventService.deleteImageEvent(id);

        return ResponseEntity.noContent().build();
    }
}