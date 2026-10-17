package com.tripnext.demo.service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.MimeTypeUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sends STOMP frames to subscribers on every backend instance.
 *
 * Each instance runs Spring's in-memory broker, which only knows its own
 * WebSocket sessions. So instead of sending locally, we publish the frame to a
 * Redis channel; every instance (including this one) receives it and delivers
 * it to its own subscribers (SRS CON-REAL-005). If Redis is unreachable we fall
 * back to local delivery so a single-instance setup keeps working.
 *
 * Frames are sent after the surrounding transaction commits (write-then-
 * broadcast), so clients never see a change that was rolled back.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeBroadcaster implements MessageListener {

    public static final String CHANNEL = "tripnest:stomp";

    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redis;

    public void send(String destination, Object payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            publish(destination, payload);
                        }
                    }
            );
        } else {
            publish(destination, payload);
        }
    }

    private void publish(String destination, Object payload) {
        String json = toJson(payload);
        try {
            redis.convertAndSend(CHANNEL, destination + "\n" + json);
        } catch (RuntimeException e) {
            log.warn("Redis pub/sub unavailable, delivering {} locally only: {}", destination, e.getMessage());
            deliverLocally(destination, json);
        }
    }

    // Called by the Redis listener container for frames from any instance.
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        int split = body.indexOf('\n');
        if (split <= 0) {
            return;
        }
        deliverLocally(body.substring(0, split), body.substring(split + 1));
    }

    private void deliverLocally(String destination, String json) {
        messagingTemplate.send(
                destination,
                MessageBuilder.withPayload(json.getBytes(StandardCharsets.UTF_8))
                        .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                        .build()
        );
    }

    // Serialize exactly as the STOMP broker would (same converter/ObjectMapper).
    private String toJson(Object payload) {
        org.springframework.messaging.Message<?> converted = messagingTemplate.getMessageConverter().toMessage(
                payload,
                new MessageHeaders(Map.of(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON))
        );
        if (converted == null) {
            throw new IllegalStateException("Cannot serialize " + payload.getClass());
        }
        Object body = converted.getPayload();
        return body instanceof byte[] bytes
                ? new String(bytes, StandardCharsets.UTF_8)
                : body.toString();
    }
}
