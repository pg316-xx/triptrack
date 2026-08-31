package com.tripnext.demo.dto.auth;
public record LoginRequest(
        String email,
        String password
) {}