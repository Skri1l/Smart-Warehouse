package com.skr1l.backend.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceRequestDto(
        @NotBlank
        String deviceKey,

        @NotNull
        Long shelfId) {
}