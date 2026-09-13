package com.tripnext.demo.service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.tripnext.demo.config.JwtService;
import com.tripnext.demo.dto.auth.AuthResponse;
import com.tripnext.demo.exception.UnauthorizedException;
import com.tripnext.demo.model.OAuthAccount;
import com.tripnext.demo.model.RefreshToken;
import com.tripnext.demo.model.User;
import com.tripnext.demo.model.help.OAuthProvider;
import com.tripnext.demo.model.help.UserStatus;
import com.tripnext.demo.repo.OAuthAccountRepo;
import com.tripnext.demo.repo.RefreshTokenRepo;
import com.tripnext.demo.repo.UserRepo;
import com.tripnext.demo.util.TokenHashUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OAuthService {

    @Value("${google.client.id}")
    private String googleClientId;

    private final UserRepo userRepo;
    private final OAuthAccountRepo oauthAccountRepo;
    private final RefreshTokenRepo refreshTokenRepo;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse verifyGoogleTokenAndLogin(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    new GsonFactory()
            )
            .setAudience(Collections.singletonList(googleClientId))
            .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();

                String providerUserId = payload.getSubject();
                String email = payload.getEmail();
                String name = (String) payload.get("name");
                String pictureUrl = (String) payload.get("picture");

                // Find or create User
                User user = userRepo.findByEmail(email).orElseGet(() -> {
                    User newUser = new User(
                            name,
                            email,
                            passwordEncoder.encode(UUID.randomUUID().toString()) // random secure password
                    );
                    newUser.setStatus(UserStatus.ACTIVE);
                    newUser.setProfileImageUrl(pictureUrl);
                    return userRepo.save(newUser);
                });

                // Check if user is suspended
                if (user.getStatus() == UserStatus.SUSPENDED) {
                    throw new UnauthorizedException("Account is suspended");
                }

                // Link OAuth account if not already linked
                Optional<OAuthAccount> oauthAccount = oauthAccountRepo
                        .findByProviderAndProviderUserId(OAuthProvider.GOOGLE, providerUserId);

                if (oauthAccount.isEmpty()) {
                    OAuthAccount newOauthAccount = new OAuthAccount(
                            user,
                            OAuthProvider.GOOGLE,
                            providerUserId
                    );
                    oauthAccountRepo.save(newOauthAccount);
                }

                // Generate JWT tokens
                String accessToken = jwtService.generateAccessToken(user.getEmail());
                String refreshToken = jwtService.generateRefreshToken(user.getEmail());

                RefreshToken token = new RefreshToken(
                        user,
                        TokenHashUtil.hash(refreshToken),
                        OffsetDateTime.now()
                            .plusSeconds(jwtService.getRefreshTokenExpiration() / 1000)
                );
                
                refreshTokenRepo.save(token);

                return new AuthResponse(
                        accessToken,
                        refreshToken
                );

            } else {
                throw new UnauthorizedException("Invalid Google ID token");
            }
        } catch (Exception e) {
            throw new UnauthorizedException("Google authentication failed: " + e.getMessage());
        }
    }
}
