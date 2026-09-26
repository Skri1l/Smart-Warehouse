package com.skr1l.backend.exception;

public record ErrorResponse(
        int status,
        String message
) {
}