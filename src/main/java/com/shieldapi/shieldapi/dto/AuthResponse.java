package com.shieldapi.shieldapi.dto;

public record AuthResponse(
        String token,
        String tokenType,
        String username,
        long expiresInMs
) {
}
