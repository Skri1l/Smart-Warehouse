package com.skr1l.backend.imageEvent.dto;

import com.skr1l.backend.imageEvent.enums.ImageProcessingStatus;
import java.time.LocalDateTime;

public record ImageEventResponseDto(
        Long id,
        Long deviceId,
        Long shelfId,
        String imagePath,
        LocalDateTime capturedAt,
        LocalDateTime receivedAt,
        ImageProcessingStatus processingStatus) {
}