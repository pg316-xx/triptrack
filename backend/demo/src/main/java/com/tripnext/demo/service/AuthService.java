package com.tripnext.demo.service;

import java.time.OffsetDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.tripnext.demo.config.JwtService;
import com.tripnext.demo.dto.auth.AuthResponse;
import com.tripnext.demo.dto.auth.LoginRequest;
import com.tripnext.demo.dto.auth.RefreshTokenRequest;
import com.tripnext.demo.dto.auth.RegisterRequest;
import com.tripnext.demo.exception.BadRequestException;
import com.tripnext.demo.exception.ResourceNotFoundException;
import com.tripnext.demo.exception.UnauthorizedException;
import com.tripnext.demo.model.RefreshToken;
import com.tripnext.demo.model.User;
import com.tripnext.demo.repo.RefreshTokenRepo;
import com.tripnext.demo.repo.UserRepo;
import com.tripnext.demo.util.TokenHashUtil;

import jakarta.transaction.Transactional;

@Service
public class AuthService {

    private final UserRepo userRepo;
    private final RefreshTokenRepo refreshTokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepo userRepo,
            RefreshTokenRepo refreshTokenRepo,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepo = userRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {

        if (userRepo.existsByEmail(request.email())) {
            throw new BadRequestException("Email already exists");
        }

        User user = new User(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password())
        );

        userRepo.save(user);

        String accessToken =
                jwtService.generateAccessToken(user.getEmail());

        String refreshToken =
                jwtService.generateRefreshToken(user.getEmail());

        RefreshToken token = new RefreshToken(
                user,
                TokenHashUtil.hash(refreshToken),
                OffsetDateTime.now()
    .plusSeconds(
        jwtService.getRefreshTokenExpiration() / 1000
    )
        );

        refreshTokenRepo.save(token);

        return new AuthResponse(
                accessToken,
                refreshToken
        );
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepo.findByEmail(request.email())
                .orElseThrow(
                        () -> new BadRequestException("User not found")
                );

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String accessToken =
                jwtService.generateAccessToken(user.getEmail());

        String refreshToken =
                jwtService.generateRefreshToken(user.getEmail());

        RefreshToken token = new RefreshToken(
                user,
                TokenHashUtil.hash(refreshToken),
                OffsetDateTime.now()
    .plusSeconds(
        jwtService.getRefreshTokenExpiration() / 1000
    )
        );

        refreshTokenRepo.save(token);

        return new AuthResponse(
                accessToken,
                refreshToken
        );
    }
    public AuthResponse refreshToken(
        RefreshTokenRequest request
) {

    String refreshToken = request.refreshToken();

    String email = jwtService.extractEmail(refreshToken);

    RefreshToken storedToken =
            refreshTokenRepo
                    .findByTokenHash(
                            TokenHashUtil.hash(refreshToken)
                    )
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Refresh token not found"
                            )
                    );

    if (Boolean.TRUE.equals(storedToken.getIsRevoked())) {
        throw new UnauthorizedException(
                "Refresh token revoked"
        );
    }

    if (storedToken.getExpiresAt()
            .isBefore(OffsetDateTime.now())) {

        throw new UnauthorizedException(
                "Refresh token expired"
        );
    }

    String newAccessToken =
            jwtService.generateAccessToken(email);

    return new AuthResponse(
            newAccessToken,
            refreshToken
    );
}
@Transactional
public void logout(
        RefreshTokenRequest request
) {

    String tokenHash =
            TokenHashUtil.hash(
                    request.refreshToken()
            );

    RefreshToken refreshToken =
            refreshTokenRepo
                    .findByTokenHash(tokenHash)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Refresh token not found"
                            )
                    );

    refreshToken.setIsRevoked(true);

    refreshTokenRepo.save(refreshToken);
}
}