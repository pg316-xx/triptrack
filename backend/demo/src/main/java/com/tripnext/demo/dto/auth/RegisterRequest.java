package com.tripnext.demo.dto.auth;

public record RegisterRequest(
        String name,
        String email,
        String password
) {}