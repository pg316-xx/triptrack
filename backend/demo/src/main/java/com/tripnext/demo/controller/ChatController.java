package com.tripnext.demo.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tripnext.demo.dto.chat.ChatMessageRequest;
import com.tripnext.demo.dto.chat.ChatMessageResponse;
import com.tripnext.demo.service.ChatService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trips/{tripId}/messages")
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<Page<ChatMessageResponse>>
    getMessages(
            @PathVariable UUID tripId,
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                chatService.getMessages(
                        tripId,
                        pageable
                )
        );
    }

    @PostMapping
    public ResponseEntity<ChatMessageResponse>
    sendMessage(
            @PathVariable UUID tripId,
            @Valid @RequestBody
            ChatMessageRequest request
    ) {

        ChatMessageResponse response =
                chatService.saveMessage(
                        tripId,
                        request.message()
                );

        return ResponseEntity.status(
                HttpStatus.CREATED
        ).body(response);
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable UUID tripId,
            @PathVariable UUID messageId
    ) {

        chatService.deleteMessage(
                tripId,
                messageId
        );

        return ResponseEntity.noContent().build();
    }
}