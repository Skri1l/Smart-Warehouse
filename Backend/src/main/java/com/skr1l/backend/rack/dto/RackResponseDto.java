package com.skr1l.backend.rack.dto;

public record RackResponseDto(
        Long id,
        String label,
        Long warehouseId
) {}
