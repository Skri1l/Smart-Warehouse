package com.skr1l.backend.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserRequestDTO(
        @NotBlank String email,
        @NotBlank String username,
        @NotBlank String password
) {
}
