package com.tripnext.demo.controller;

import java.util.UUID;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.tripnext.demo.dto.chat.ChatMessageResponse;
import com.tripnext.demo.dto.chat.SendMessageRequest;
import com.tripnext.demo.service.ChatService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/trip/{tripId}/chat")
    public void sendMessage(

            @DestinationVariable
            UUID tripId,

            SendMessageRequest request
    ) {

        ChatMessageResponse response =
                chatService.saveMessage(
                        tripId,
                        request.getMessage()
                );

        messagingTemplate.convertAndSend(
                "/topic/trip/" + tripId + "/chat",
                response
        );
    }
}