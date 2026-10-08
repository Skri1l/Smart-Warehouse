package com.skr1l.backend.vision.dto;

public record CameraImageResponseDto(
  Long imageEventId,
  Long recognitionResultId,
  Long shelfId,
  Integer previousQuantity,
  Integer quantity,
  Integer quantityDelta
) {
}
