package com.skr1l.backend.device.dto;

import com.skr1l.backend.device.enums.DeviceStatus;
import java.time.LocalDateTime;

public record DeviceResponseDto(
        Long id,
        String deviceKey,
        DeviceStatus status,
        LocalDateTime lastSeenAt,
        Long shelfId) {
}