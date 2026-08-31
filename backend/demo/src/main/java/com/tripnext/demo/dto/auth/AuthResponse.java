package com.tripnext.demo.dto.auth;

public record AuthResponse(
        String accessToken,
        String refreshToken
) {}