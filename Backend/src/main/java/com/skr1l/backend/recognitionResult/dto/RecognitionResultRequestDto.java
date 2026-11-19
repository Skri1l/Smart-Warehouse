package com.skr1l.backend.recognitionResult.dto;

import jakarta.validation.constraints.NotNull;

public record RecognitionResultRequestDto(
        @NotNull Long imageEventId,
        @NotNull Long productId,
        @NotNull Integer detectedQuantity,
        @NotNull Integer quantityDelta,
        @NotNull Double confidence) {
}