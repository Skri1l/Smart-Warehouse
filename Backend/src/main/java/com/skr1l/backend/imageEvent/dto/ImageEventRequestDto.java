package com.skr1l.backend.imageEvent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ImageEventRequestDto(

        @NotNull Long deviceId,

        @NotBlank String imagePath,

        @NotNull LocalDateTime capturedAt) {
}