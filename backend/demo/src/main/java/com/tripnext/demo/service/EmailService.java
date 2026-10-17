package com.tripnext.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Outgoing email over SMTP (SRS 7.6). Locally this goes to Mailpit; on AWS
 * point SMTP_HOST/SMTP_USERNAME/SMTP_PASSWORD at Amazon SES's SMTP endpoint.
 *
 * Sends are @Async and never throw: a mail failure is logged and must not
 * fail or slow down the HTTP request that triggered it (EXT-EMAIL-002/003).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.enabled:true}")
    private boolean enabled;

    @Async
    public void send(String to, String subject, String body) {
        if (!enabled) {
            log.info("Mail disabled; would send '{}' to {}", subject, to);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Sent '{}' to {}", subject, to);
        } catch (Exception e) {
            log.warn("Failed to send '{}' to {}: {}", subject, to, e.getMessage());
        }
    }

    public void sendPasswordReset(String to, String name, String link, long validMinutes) {
        send(to, "Reset your TripNest password", """
                Hi %s,

                We received a request to reset your TripNest password.
                Open this link to choose a new one (it expires in %d minutes):

                %s

                If you didn't ask for this, you can ignore this email; your password won't change.

                — TripNest
                """.formatted(name, validMinutes, link));
    }

    public void sendTripInvitation(String to, String inviterName, String tripTitle, String link) {
        send(to, inviterName + " invited you to \"" + tripTitle + "\" on TripNest", """
                Hi,

                %s invited you to plan "%s" together on TripNest.
                Join the trip with this link:

                %s

                — TripNest
                """.formatted(inviterName, tripTitle, link));
    }
}
