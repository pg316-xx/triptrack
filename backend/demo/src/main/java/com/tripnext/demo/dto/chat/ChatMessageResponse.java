package com.tripnext.demo.dto.chat;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ChatMessageResponse(

        UUID id,
        UUID senderId,
        String senderName,
        String message,
        OffsetDateTime createdAt

) {
}