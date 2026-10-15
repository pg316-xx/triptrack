package com.tripnext.demo.service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.tripnext.demo.exception.BadRequestException;
import com.tripnext.demo.exception.ConflictException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Idempotency-Key handling backed by Redis (SRS FR-EXP-002, 24h TTL).
 *
 * Redis key: idempotency:{scope}:{userId}:{Idempotency-Key}
 *   - "PENDING" while the first request is running (a duplicate gets 409)
 *   - the created resource id once the transaction commits (a duplicate
 *     gets that same resource back, with no new DB writes)
 *   - deleted if the transaction rolls back, so the client can retry
 *
 * If Redis is unavailable the request still runs, just without duplicate
 * protection (SRS 7.11: "bypass caching without blocking core logic").
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    static final Duration TTL = Duration.ofHours(24);
    private static final String PENDING = "PENDING";
    private static final int MAX_KEY_LENGTH = 100;

    private final StringRedisTemplate redis;

    /**
     * @param action runs at most once per key; must be called inside the
     *               caller's @Transactional method
     * @param idOf   extracts the created resource's id from the result
     * @param replay rebuilds the response for an already-processed key
     */
    public <T> T execute(
            String scope,
            UUID userId,
            String idempotencyKey,
            Supplier<T> action,
            Function<T, UUID> idOf,
            Function<UUID, T> replay
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required");
        }
        if (idempotencyKey.length() > MAX_KEY_LENGTH) {
            throw new BadRequestException("Idempotency-Key is too long");
        }

        String redisKey = "idempotency:" + scope + ":" + userId + ":" + idempotencyKey;

        Boolean claimed;
        try {
            claimed = redis.opsForValue().setIfAbsent(redisKey, PENDING, TTL);
        } catch (RuntimeException e) {
            log.warn("Redis unavailable, running {} without idempotency: {}", scope, e.getMessage());
            return action.get();
        }

        if (!Boolean.TRUE.equals(claimed)) {
            String existing = safeGet(redisKey);
            if (existing == null || PENDING.equals(existing)) {
                throw new ConflictException(
                        "A request with this Idempotency-Key is still being processed"
                );
            }
            return replay.apply(UUID.fromString(existing));
        }

        AtomicReference<UUID> createdId = new AtomicReference<>();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            safeSet(redisKey, createdId.get());
                        }

                        @Override
                        public void afterCompletion(int status) {
                            if (status != STATUS_COMMITTED) {
                                safeDelete(redisKey);
                            }
                        }
                    }
            );
            T result = action.get();
            createdId.set(idOf.apply(result));
            return result;
        }

        // No surrounding transaction: record the result immediately.
        try {
            T result = action.get();
            safeSet(redisKey, idOf.apply(result));
            return result;
        } catch (RuntimeException e) {
            safeDelete(redisKey);
            throw e;
        }
    }

    private String safeGet(String key) {
        try {
            return redis.opsForValue().get(key);
        } catch (RuntimeException e) {
            log.warn("Redis read failed for {}: {}", key, e.getMessage());
            return null;
        }
    }

    private void safeSet(String key, UUID id) {
        if (id == null) {
            safeDelete(key);
            return;
        }
        try {
            redis.opsForValue().set(key, id.toString(), TTL);
        } catch (RuntimeException e) {
            log.warn("Redis write failed for {}: {}", key, e.getMessage());
        }
    }

    private void safeDelete(String key) {
        try {
            redis.delete(key);
        } catch (RuntimeException e) {
            log.warn("Redis delete failed for {}: {}", key, e.getMessage());
        }
    }
}
