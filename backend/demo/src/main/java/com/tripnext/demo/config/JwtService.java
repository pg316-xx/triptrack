package com.tripnext.demo.config;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(
            secret.getBytes(StandardCharsets.UTF_8)
    );
    }

    public String generateAccessToken(String email) {

        Date now = new Date();

        Date expiry = new Date(
                now.getTime() + accessTokenExpiration
        );

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(String email) {

        Date now = new Date();

        Date expiry = new Date(
                now.getTime() + refreshTokenExpiration
        );

        // A unique jti makes every token distinct. Without it, two tokens for
        // the same user in the same second (register, then the immediate
        // login) are byte-identical and collide on refresh_tokens.token_hash.
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {

        return extractClaims(token)
                .getSubject();
    }

    public boolean isTokenValid(String token, String email) {

        return email.equals(extractEmail(token))
                && !extractClaims(token)
                        .getExpiration()
                        .before(new Date());
    }

    private Claims extractClaims(String token) {

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    public long getRefreshTokenExpiration() {
    return refreshTokenExpiration;
    }
}
