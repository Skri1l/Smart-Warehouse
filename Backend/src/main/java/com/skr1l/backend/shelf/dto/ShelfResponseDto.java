package com.skr1l.backend.shelf.dto;

import com.skr1l.backend.shelf.enums.ShelfStatus;

public record ShelfResponseDto(
        Long id,
        String label,
        Integer capacity,
        ShelfStatus status,
        Long rackId) {
}