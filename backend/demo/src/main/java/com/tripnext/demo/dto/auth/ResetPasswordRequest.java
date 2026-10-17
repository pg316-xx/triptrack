package com.tripnext.demo.dto.auth;

public record ResetPasswordRequest(
        String token,
        String newPassword
) {}
