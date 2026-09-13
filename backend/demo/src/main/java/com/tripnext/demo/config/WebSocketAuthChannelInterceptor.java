package com.tripnext.demo.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor
        implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel
    ) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        if (accessor != null
                && StompCommand.CONNECT.equals(
                        accessor.getCommand()
                )) {

            String token = extractToken(accessor);

            if (token != null) {

                String email =
                        jwtService.extractEmail(token);

                if (email != null
                        && jwtService.isTokenValid(
                                token, email
                        )) {

                    UserDetails userDetails =
                            userDetailsService
                                    .loadUserByUsername(
                                            email
                                    );

                    UsernamePasswordAuthenticationToken
                            authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails
                                            .getAuthorities()
                            );

                    accessor.setUser(authentication);
                }
            }
        }

        return message;
    }

    private String extractToken(
            StompHeaderAccessor accessor
    ) {

        String authHeader =
                accessor.getFirstNativeHeader(
                        "Authorization"
                );

        if (authHeader != null
                && authHeader.startsWith("Bearer ")) {

            return authHeader.substring(7);
        }

        return null;
    }
}
