package com.tripnext.demo.dto.chat;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageRequest(

        @NotBlank
        String message

) {
}