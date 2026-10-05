package com.skr1l.backend.recognitionResult.controller;

import com.skr1l.backend.recognitionResult.dto.RecognitionResultRequestDto;
import com.skr1l.backend.recognitionResult.dto.RecognitionResultResponseDto;
import com.skr1l.backend.recognitionResult.enums.RecognitionStatus;
import com.skr1l.backend.recognitionResult.service.RecognitionResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "Recognition Results", description = "Recognition result management API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/recognition-results")
public class RecognitionResultController {

    private final RecognitionResultService recognitionResultService;

    @Operation(summary = "Create a recognition result")
    @ApiResponse(responseCode = "201", description = "Recognition result created")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "404", description = "Image event or product not found")
    @PostMapping
    public ResponseEntity<Long> createRecognitionResult(
            @Valid @RequestBody RecognitionResultRequestDto dto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recognitionResultService.createRecognitionResult(dto));
    }

    @Operation(summary = "Get recognition result by id")
    @ApiResponse(responseCode = "200", description = "Recognition result found")
    @ApiResponse(responseCode = "404", description = "Recognition result not found")
    @GetMapping("/{id}")
    public ResponseEntity<RecognitionResultResponseDto> getRecognitionResultById(@PathVariable Long id) {

        return ResponseEntity.ok(recognitionResultService
                .getRecognitionResultById(id));
    }

    @Operation(summary = "Get all recognition results")
    @ApiResponse(responseCode = "200", description = "Recognition results returned successfully")
    @GetMapping
    public ResponseEntity<List<RecognitionResultResponseDto>> getAllRecognitionResults() {

        return ResponseEntity.ok(recognitionResultService
                .getAllRecognitionResults());
    }

    @Operation(summary = "Get recognition results by image event id")
    @ApiResponse(responseCode = "200", description = "Recognition results returned successfully")
    @GetMapping("/image-event/{imageEventId}")
    public ResponseEntity<List<RecognitionResultResponseDto>> getRecognitionResultsByImageEventId(
            @PathVariable Long imageEventId) {

        return ResponseEntity.ok(recognitionResultService
                .getRecognitionResultsByImageEventId(imageEventId));
    }

    @Operation(summary = "Get recognition results by product id")
    @ApiResponse(responseCode = "200", description = "Recognition results returned successfully")
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<RecognitionResultResponseDto>> getRecognitionResultsByProductId(
            @PathVariable Long productId) {

        return ResponseEntity.ok(recognitionResultService
                .getRecognitionResultsByProductId(productId));
    }

    @Operation(summary = "Get recognition results by status")
    @ApiResponse(responseCode = "200", description = "Recognition results returned successfully")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<RecognitionResultResponseDto>> getRecognitionResultsByStatus(
            @PathVariable RecognitionStatus status) {

        return ResponseEntity.ok(recognitionResultService
                .getRecognitionResultsByStatus(status));
    }

    @Operation(summary = "Get recognition results by minimum confidence")
    @ApiResponse(responseCode = "200", description = "Recognition results returned successfully")
    @GetMapping("/confidence")
    public ResponseEntity<List<RecognitionResultResponseDto>> getRecognitionResultsByMinConfidence(
            @RequestParam Double min) {

        return ResponseEntity.ok(recognitionResultService
                .getRecognitionResultsByMinConfidence(min));
    }

    @Operation(summary = "Confirm a recognition result")
    @ApiResponse(responseCode = "204", description = "Recognition result confirmed")
    @ApiResponse(responseCode = "404", description = "Recognition result not found")
    @PatchMapping("/{id}/confirm")
    public ResponseEntity<Void> confirmRecognitionResult(@PathVariable Long id) {
        recognitionResultService.confirmRecognitionResult(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reject a recognition result")
    @ApiResponse(responseCode = "204", description = "Recognition result rejected")
    @ApiResponse(responseCode = "404", description = "Recognition result not found")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<Void> rejectRecognitionResult(@PathVariable Long id) {
        recognitionResultService.rejectRecognitionResult(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Mark recognition result as failed")
    @ApiResponse(responseCode = "204", description = "Recognition result marked as failed")
    @ApiResponse(responseCode = "404", description = "Recognition result not found")
    @PatchMapping("/{id}/failed")
    public ResponseEntity<Void> markRecognitionResultAsFailed(@PathVariable Long id) {
        recognitionResultService.markRecognitionResultAsFailed(id);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete a recognition result")
    @ApiResponse(responseCode = "204", description = "Recognition result deleted")
    @ApiResponse(responseCode = "404", description = "Recognition result not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecognitionResult(@PathVariable Long id) {
        recognitionResultService.deleteRecognitionResult(id);

        return ResponseEntity.noContent().build();
    }
}