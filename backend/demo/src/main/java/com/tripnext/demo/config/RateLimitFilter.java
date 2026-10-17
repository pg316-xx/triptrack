package com.tripnext.demo.config;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Per-client-IP limits on the sensitive auth endpoints (SRS SEC-006), as a
 * fixed window counted in Redis so the limit is shared by every backend
 * instance. If Redis is down, requests are let through (fail open) rather
 * than locking everybody out.
 *
 * Disable with RATE_LIMIT_ENABLED=false (e.g. for load tests that register
 * many users from one machine).
 */
@Slf4j
@Component
// After the Spring Security chain (order -100) so 429s still carry CORS headers.
@Order(-50)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private record Rule(String name, String method, String path, int limit, Duration window) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule("login", "POST", "/api/auth/login", 10, Duration.ofMinutes(1)),
            new Rule("google", "POST", "/api/auth/google-login", 10, Duration.ofMinutes(1)),
            new Rule("register", "POST", "/api/auth/register", 5, Duration.ofMinutes(10)),
            new Rule("forgot", "POST", "/api/auth/forgot-password", 5, Duration.ofMinutes(15)),
            new Rule("reset", "POST", "/api/auth/reset-password", 10, Duration.ofMinutes(15)),
            new Rule("refresh", "POST", "/api/auth/refresh", 30, Duration.ofMinutes(1))
    );

    private final StringRedisTemplate redis;

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    // Behind an AWS ALB the client IP is in X-Forwarded-For; only trust it when configured.
    @Value("${app.rate-limit.trust-forwarded-for:false}")
    private boolean trustForwardedFor;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        Rule rule = enabled ? match(request) : null;
        if (rule != null) {
            String key = "ratelimit:" + rule.name() + ":" + clientIp(request);
            try {
                Long count = redis.opsForValue().increment(key);
                if (count != null && count == 1) {
                    redis.expire(key, rule.window());
                }
                if (count != null && count > rule.limit()) {
                    Long ttl = redis.getExpire(key);
                    long retryAfter = ttl == null || ttl < 1 ? rule.window().toSeconds() : ttl;
                    response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
                    ProblemJson.write(request, response, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                            "Too many attempts. Try again in " + retryAfter + " seconds.");
                    return;
                }
            } catch (RuntimeException e) {
                log.warn("Rate limiter unavailable, allowing request: {}", e.getMessage());
            }
        }
        chain.doFilter(request, response);
    }

    private Rule match(HttpServletRequest request) {
        for (Rule rule : RULES) {
            if (rule.method().equals(request.getMethod()) && rule.path().equals(request.getRequestURI())) {
                return rule;
            }
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
