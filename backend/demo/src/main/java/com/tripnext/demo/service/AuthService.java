package com.tripnext.demo.service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.tripnext.demo.config.JwtService;
import com.tripnext.demo.dto.auth.AuthResponse;
import com.tripnext.demo.dto.auth.ForgotPasswordRequest;
import com.tripnext.demo.dto.auth.LoginRequest;
import com.tripnext.demo.dto.auth.RefreshTokenRequest;
import com.tripnext.demo.dto.auth.RegisterRequest;
import com.tripnext.demo.dto.auth.ResetPasswordRequest;
import com.tripnext.demo.exception.BadRequestException;
import com.tripnext.demo.exception.UnauthorizedException;
import com.tripnext.demo.model.PasswordResetToken;
import com.tripnext.demo.model.RefreshToken;
import com.tripnext.demo.model.User;
import com.tripnext.demo.model.help.UserStatus;
import com.tripnext.demo.repo.PasswordResetTokenRepo;
import com.tripnext.demo.repo.RefreshTokenRepo;
import com.tripnext.demo.repo.UserRepo;
import com.tripnext.demo.util.TokenHashUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_PASSWORD = 8;
    private static final long RESET_VALID_MINUTES = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepo userRepo;
    private final RefreshTokenRepo refreshTokenRepo;
    private final PasswordResetTokenRepo passwordResetTokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String name = request.name() == null ? "" : request.name().trim();
        String email = request.email() == null ? "" : request.email().trim();

        if (name.length() < 2 || name.length() > 100) {
            throw new BadRequestException("Name must be 2-100 characters");
        }
        if (!EMAIL.matcher(email).matches()) {
            throw new BadRequestException("Enter a valid email address");
        }
        validatePassword(request.password());
        if (userRepo.existsByEmail(email)) {
            throw new BadRequestException("Email already exists");
        }

        User user = userRepo.save(new User(name, email, passwordEncoder.encode(request.password())));
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Same answer for "no such user" and "wrong password", so the endpoint
        // can't be used to find out which emails have accounts.
        User user = userRepo.findByEmail(request.email() == null ? "" : request.email().trim())
                .filter(u -> request.password() != null
                        && passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("This account is " + user.getStatus().name().toLowerCase());
        }
        return issueTokens(user);
    }

    /**
     * Refresh-token rotation (SRS FR-AUTH-002): every refresh revokes the
     * presented token and returns a new pair. If an already-revoked token is
     * presented again, someone is replaying a stolen token, so every session
     * of that user is revoked.
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new UnauthorizedException("Refresh token is required");
        }

        RefreshToken stored = refreshTokenRepo
                .findByTokenHash(TokenHashUtil.hash(request.refreshToken()))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        User user = stored.getUser();

        if (Boolean.TRUE.equals(stored.getIsRevoked())) {
            int revoked = refreshTokenRepo.revokeAllForUser(user);
            log.warn("Revoked refresh token reused for user {}; revoked {} other sessions", user.getId(), revoked);
            throw new UnauthorizedException("Refresh token revoked");
        }
        if (stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("Refresh token expired");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("This account is " + user.getStatus().name().toLowerCase());
        }

        stored.setIsRevoked(true);
        refreshTokenRepo.save(stored);
        return issueTokens(user);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        if (request.refreshToken() == null) {
            return;
        }
        // Idempotent: logging out with an unknown/old token is not an error.
        refreshTokenRepo.findByTokenHash(TokenHashUtil.hash(request.refreshToken()))
                .ifPresent(token -> {
                    token.setIsRevoked(true);
                    refreshTokenRepo.save(token);
                });
    }

    /**
     * Always succeeds, whether or not the email has an account, so it can't be
     * used to probe for users. If it does, a one-time link is emailed.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.email() == null ? "" : request.email().trim();

        userRepo.findByEmail(email)
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .ifPresent(user -> {
                    passwordResetTokenRepo.invalidateAllForUser(user);

                    byte[] raw = new byte[32];
                    RANDOM.nextBytes(raw);
                    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

                    passwordResetTokenRepo.save(new PasswordResetToken(
                            user,
                            TokenHashUtil.hash(token),
                            OffsetDateTime.now().plusMinutes(RESET_VALID_MINUTES)
                    ));

                    emailService.sendPasswordReset(
                            user.getEmail(),
                            user.getName(),
                            frontendUrl + "/reset-password?token=" + token,
                            RESET_VALID_MINUTES
                    );
                });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (request.token() == null || request.token().isBlank()) {
            throw new BadRequestException("Reset token is required");
        }
        validatePassword(request.newPassword());

        PasswordResetToken reset = passwordResetTokenRepo
                .findByTokenHash(TokenHashUtil.hash(request.token()))
                .filter(t -> t.getUsedAt() == null && t.getExpiresAt().isAfter(OffsetDateTime.now()))
                .orElseThrow(() -> new BadRequestException("INVALID_RESET_TOKEN: This reset link is invalid or has expired"));

        User user = reset.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepo.save(user);

        reset.setUsedAt(OffsetDateTime.now());
        passwordResetTokenRepo.save(reset);
        passwordResetTokenRepo.invalidateAllForUser(user);

        // Sign out every device that had the old password.
        refreshTokenRepo.revokeAllForUser(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        refreshTokenRepo.save(new RefreshToken(
                user,
                TokenHashUtil.hash(refreshToken),
                OffsetDateTime.now().plusSeconds(jwtService.getRefreshTokenExpiration() / 1000)
        ));
        return new AuthResponse(accessToken, refreshToken);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD) {
            throw new BadRequestException("Password must be at least " + MIN_PASSWORD + " characters");
        }
        if (password.length() > 72) {
            // BCrypt ignores everything after 72 bytes
            throw new BadRequestException("Password must be at most 72 characters");
        }
    }
}
