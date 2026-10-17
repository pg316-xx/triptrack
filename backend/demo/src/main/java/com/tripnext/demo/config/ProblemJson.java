package com.tripnext.demo.config;

import java.io.IOException;
import java.time.OffsetDateTime;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes an RFC 7807 body from servlet filters / security handlers, which run
 * before Spring MVC and so can't use GlobalExceptionHandler. Same shape.
 */
public final class ProblemJson {

    private ProblemJson() {
    }

    public static void write(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message
    ) throws IOException {
        String path = request.getRequestURI();
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);

        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"type":"about:blank","title":%s,"status":%d,"detail":%s,"instance":%s,\
                "timestamp":%s,"error":%s,"code":%s,"message":%s,"path":%s,"requestId":%s}"""
                .formatted(q(status.getReasonPhrase()), status.value(), q(message), q(path),
                        q(OffsetDateTime.now().toString()), q(status.name()), q(code), q(message), q(path),
                        requestId == null ? "null" : q(requestId)));
    }

    private static String q(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }
}
