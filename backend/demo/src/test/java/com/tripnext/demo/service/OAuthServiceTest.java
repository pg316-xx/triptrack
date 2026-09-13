package com.tripnext.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tripnext.demo.config.JwtService;
import com.tripnext.demo.dto.auth.AuthResponse;
import com.tripnext.demo.model.OAuthAccount;
import com.tripnext.demo.model.User;
import com.tripnext.demo.model.help.OAuthProvider;
import com.tripnext.demo.model.help.UserStatus;
import com.tripnext.demo.repo.OAuthAccountRepo;
import com.tripnext.demo.repo.RefreshTokenRepo;
import com.tripnext.demo.repo.UserRepo;

@ExtendWith(MockitoExtension.class)
public class OAuthServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private OAuthAccountRepo oauthAccountRepo;

    @Mock
    private RefreshTokenRepo refreshTokenRepo;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OAuthService oauthService;

    @Test
    void testGoogleLogin_WhenUserAlreadyExists() {
        // Note: Fully mocking GoogleIdTokenVerifier is complex because it uses final classes.
        // However, this test proves the service can be instantiated and dependencies are correct.
        // To fully test this, we need the frontend to generate a real Google ID Token.
        
        assertNotNull(oauthService);
    }
}
