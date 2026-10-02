package com.skr1l.backend.recognitionResult.controller;

import com.skr1l.backend.recognitionResult.dto.RecognitionResultRequestDto;
import com.skr1l.backend.recognitionResult.dto.RecognitionResultResponseDto;
import com.skr1l.backend.recognitionResult.enums.RecognitionStatus;
import com.skr1l.backend.recognitionResult.service.RecognitionResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/recognition-results")
public class RecognitionResultController {

    private final RecognitionResultService recognitionResultService;

    @PostMapping
    public ResponseEntity<Long> createRecognitionResult(
            @Valid @RequestBody RecognitionResultRequestDto dto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recognitionResultService.createRecognitionResult(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecognitionResultResponseDto> getRecognitionResultById(@PathVariable Long id) {
        return ResponseEntity.ok(recognitionResultService.getRecognitionResultById(id));
    }

    @GetMapping
    public ResponseEntity<List<RecognitionResultResponseDto>> getAllRecognitionResults() {
        return ResponseEntity.ok(recognitionResultService.getAllRecognitionResults());
    }

    @GetMapping("/image-event/{imageEventId}")
    public ResponseEntity<List<RecognitionResultResponseDto>> getRecognitionResultsByImageEventId(
            @PathVariable Long imageEventId) {

        return ResponseEntity.ok(recognitionResultService.getRecognitionResultsByImageEventId(imageEventId));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<RecognitionResultResponseDto>> getRecognitionResultsByProductId(
            @PathVariable Long productId) {

        return ResponseEntity.ok(recognitionResultService.getRecognitionResultsByProductId(productId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<RecognitionResultResponseDto>> getRecognitionResultsByStatus(
            @PathVariable RecognitionStatus status) {

        return ResponseEntity.ok(recognitionResultService.getRecognitionResultsByStatus(status));
    }

    @GetMapping("/confidence")
    public ResponseEntity<List<RecognitionResultResponseDto>> getRecognitionResultsByMinConfidence(
            @RequestParam Double min) {

        return ResponseEntity.ok(recognitionResultService.getRecognitionResultsByMinConfidence(min));
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<Void> confirmRecognitionResult(@PathVariable Long id) {

        recognitionResultService.confirmRecognitionResult(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<Void> rejectRecognitionResult(@PathVariable Long id) {

        recognitionResultService.rejectRecognitionResult(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/failed")
    public ResponseEntity<Void> markRecognitionResultAsFailed(@PathVariable Long id) {

        recognitionResultService.markRecognitionResultAsFailed(id);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecognitionResult(@PathVariable Long id) {

        recognitionResultService.deleteRecognitionResult(id);

        return ResponseEntity.noContent().build();
    }
}