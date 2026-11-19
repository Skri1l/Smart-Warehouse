package com.skr1l.backend.user.dto;

import com.skr1l.backend.user.enums.UserRole;
import com.skr1l.backend.user.enums.UserStatus;

public record UserResponseDTO(
        Long id,
        String email,
        String username,
        UserRole role,
        UserStatus status
) {
}
