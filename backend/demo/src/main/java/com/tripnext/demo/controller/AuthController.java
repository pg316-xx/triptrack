package com.tripnext.demo.controller;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripnext.demo.dto.auth.AuthResponse;
import com.tripnext.demo.dto.auth.LoginRequest;
import com.tripnext.demo.dto.auth.RefreshTokenRequest;
import com.tripnext.demo.dto.auth.RegisterRequest;
import com.tripnext.demo.service.AuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(
            @RequestBody RegisterRequest request
    ) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @RequestBody LoginRequest request
    ) {
        return authService.login(request);
    }
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
        @RequestBody RefreshTokenRequest request
    ) {

    return ResponseEntity.ok(
            authService.refreshToken(request)
    );
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @RequestBody RefreshTokenRequest request
    ) {

    authService.logout(request);

    return ResponseEntity.ok().build();
    }
    
    
}