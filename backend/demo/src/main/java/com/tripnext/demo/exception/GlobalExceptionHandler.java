package com.tripnext.demo.exception;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.tripnext.demo.config.RequestIdFilter;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Every error is an RFC 7807 Problem Detail (application/problem+json), with
 * the extra fields from SRS NFR-SEC-004:
 *
 * { "type": "about:blank", "title": "Bad Request", "status": 400,
 *   "detail": "...", "instance": "/api/trips/1/expenses",
 *   "timestamp": "...", "error": "BAD_REQUEST", "code": "INVALID_SPLIT_SUM",
 *   "message": "...", "path": "/api/trips/1/expenses", "requestId": "..." }
 *
 * A message written as "SOME_CODE: human text" sets code=SOME_CODE.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final Pattern CODED = Pattern.compile("^([A-Z][A-Z0-9_]{2,}):\\s*(.*)$", Pattern.DOTALL);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> notFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> noRoute(NoResourceFoundException ex, HttpServletRequest req) {
        return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "No endpoint " + req.getMethod() + " " + req.getRequestURI(), req);
    }

    @ExceptionHandler({BadRequestException.class, IllegalArgumentException.class})
    public ResponseEntity<ProblemDetail> badRequest(RuntimeException ex, HttpServletRequest req) {
        return problem(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> invalid(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        String message = fields.isEmpty()
                ? "Validation failed"
                : fields.entrySet().stream().map(e -> e.getKey() + " " + e.getValue()).reduce((a, b) -> a + "; " + b).get();
        ResponseEntity<ProblemDetail> response = problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, req);
        response.getBody().setProperty("fieldErrors", fields);
        return response;
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
    })
    public ResponseEntity<ProblemDetail> malformed(Exception ex, HttpServletRequest req) {
        String message = switch (ex) {
            case MissingServletRequestParameterException m -> m.getParameterName() + " parameter is required";
            case MethodArgumentTypeMismatchException m -> "Invalid value for " + m.getName();
            default -> "Malformed request body";
        };
        return problem(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, req);
    }

    // e.g. a missing Idempotency-Key header
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ProblemDetail> missingHeader(MissingRequestHeaderException ex, HttpServletRequest req) {
        return problem(HttpStatus.BAD_REQUEST, "MISSING_HEADER", ex.getHeaderName() + " header is required", req);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ProblemDetail> unauthorized(UnauthorizedException ex, HttpServletRequest req) {
        return problem(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> forbidden(AccessDeniedException ex, HttpServletRequest req) {
        return problem(HttpStatus.FORBIDDEN, "FORBIDDEN", "You don't have access to this resource", req);
    }

    // Optimistic locking: our explicit version check or Hibernate's @Version at flush.
    @ExceptionHandler({ConflictException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ProblemDetail> conflict(RuntimeException ex, HttpServletRequest req) {
        String message = ex instanceof ConflictException
                ? ex.getMessage()
                : "This item was modified by someone else. Reload and try again.";
        return problem(HttpStatus.CONFLICT, "CONFLICT", message, req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> integrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Data integrity violation on {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "DATA_CONFLICT", "This conflicts with existing data", req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> method(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", ex.getMethod() + " is not supported here", req);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> mediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Unsupported Content-Type", req);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> tooLarge(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return problem(HttpStatus.CONTENT_TOO_LARGE, "FILE_TOO_LARGE", "File is too large", req);
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ProblemDetail> rateLimited(RateLimitException ex, HttpServletRequest req) {
        ResponseEntity<ProblemDetail> response = problem(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", ex.getMessage(), req);
        return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(response.getBody());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> unexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Something went wrong", req);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String defaultCode, String message, HttpServletRequest req) {
        String code = defaultCode;
        String text = message == null || message.isBlank() ? status.getReasonPhrase() : message;
        Matcher m = CODED.matcher(text);
        if (m.matches()) {
            code = m.group(1);
            text = m.group(2);
        }

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, text);
        pd.setTitle(status.getReasonPhrase());
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("timestamp", OffsetDateTime.now());
        pd.setProperty("error", status.name());
        pd.setProperty("code", code);
        pd.setProperty("message", text); // kept for clients that read "message"
        pd.setProperty("path", req.getRequestURI());
        pd.setProperty("requestId", MDC.get(RequestIdFilter.MDC_KEY));

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(pd);
    }
}
