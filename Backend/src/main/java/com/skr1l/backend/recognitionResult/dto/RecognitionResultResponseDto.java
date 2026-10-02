package com.skr1l.backend.recognitionResult.dto;

import com.skr1l.backend.recognitionResult.enums.RecognitionStatus;

public record RecognitionResultResponseDto(
        Long id,
        Long imageEventId,
        Long productId,
        Integer detectedQuantity,
        Integer quantityDelta,
        Double confidence,
        RecognitionStatus status) {
}