package com.tripnext.demo.config;

import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import com.tripnext.demo.model.User;
import com.tripnext.demo.repo.TripMemberRepo;
import com.tripnext.demo.repo.UserRepo;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor
        implements ExecutorChannelInterceptor {

    private static final Pattern USER_NOTIFICATIONS =
            Pattern.compile("^/topic/user/([^/]+)/notifications$");
    private static final Pattern TRIP_TOPIC =
            Pattern.compile("^/topic/trip/([^/]+)/[^/]+$");

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepo userRepo;
    private final TripMemberRepo tripMemberRepo;

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

        if (accessor != null
                && StompCommand.SUBSCRIBE.equals(
                        accessor.getCommand()
                )) {

            authorizeSubscription(
                    accessor.getUser(),
                    accessor.getDestination()
            );
        }

        return message;
    }

    // Only allow subscribing to your own notification topic and to topics
    // of trips you are a member of. Throwing here sends a STOMP ERROR frame.
    private void authorizeSubscription(
            Principal principal,
            String destination
    ) {

        if (!(principal instanceof Authentication authentication)
                || destination == null) {
            throw new AccessDeniedException(
                    "Unauthenticated subscription"
            );
        }

        User user =
                userRepo.findByEmail(authentication.getName())
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "Unknown user"
                                )
                        );

        Matcher userTopic =
                USER_NOTIFICATIONS.matcher(destination);
        if (userTopic.matches()) {
            if (!user.getId().toString().equals(userTopic.group(1))) {
                throw new AccessDeniedException(
                        "Cannot subscribe to another user's notifications"
                );
            }
            return;
        }

        Matcher tripTopic =
                TRIP_TOPIC.matcher(destination);
        if (tripTopic.matches()) {
            UUID tripId = parseUuid(tripTopic.group(1));
            if (tripId == null
                    || !tripMemberRepo.existsByTrip_IdAndUser_Id(
                            tripId, user.getId()
                    )) {
                throw new AccessDeniedException(
                        "Not a member of this trip"
                );
            }
            return;
        }

        throw new AccessDeniedException(
                "Subscription not allowed: " + destination
        );
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // @MessageMapping handlers run on a broker executor thread, so the
    // session user set on CONNECT must be copied into the SecurityContext
    // for each message, then cleared once the handler finishes.
    @Override
    public Message<?> beforeHandle(
            Message<?> message,
            MessageChannel channel,
            MessageHandler handler
    ) {

        Principal user =
                SimpMessageHeaderAccessor.getUser(
                        message.getHeaders()
                );

        if (user instanceof Authentication authentication) {

            SecurityContext context =
                    SecurityContextHolder
                            .createEmptyContext();

            context.setAuthentication(authentication);

            SecurityContextHolder.setContext(context);
        }

        return message;
    }

    @Override
    public void afterMessageHandled(
            Message<?> message,
            MessageChannel channel,
            MessageHandler handler,
            Exception ex
    ) {

        SecurityContextHolder.clearContext();
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
