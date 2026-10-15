package com.tripnext.demo.exception;

import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(
            ResourceNotFoundException.class
    )
    public ResponseEntity<ErrorResponse>
    handleResourceNotFound(
            ResourceNotFoundException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        new ErrorResponse(
                                ex.getMessage(),
                                OffsetDateTime.now()
                        )
                );
    }

    @ExceptionHandler(
            BadRequestException.class
    )
    public ResponseEntity<ErrorResponse>
    handleBadRequest(
            BadRequestException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        new ErrorResponse(
                                ex.getMessage(),
                                OffsetDateTime.now()
                        )
                );
    }

    @ExceptionHandler(
            UnauthorizedException.class
    )
    public ResponseEntity<ErrorResponse>
    handleUnauthorized(
            UnauthorizedException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                        new ErrorResponse(
                                ex.getMessage(),
                                OffsetDateTime.now()
                        )
                );
    }

    // e.g. a missing Idempotency-Key header
    @ExceptionHandler(
            MissingRequestHeaderException.class
    )
    public ResponseEntity<ErrorResponse>
    handleMissingHeader(
            MissingRequestHeaderException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        new ErrorResponse(
                                ex.getHeaderName() + " header is required",
                                OffsetDateTime.now()
                        )
                );
    }

    // Optimistic locking: either our explicit version check or Hibernate's
    // @Version check at flush time when two writes race.
    @ExceptionHandler({
            ConflictException.class,
            ObjectOptimisticLockingFailureException.class
    })
    public ResponseEntity<ErrorResponse>
    handleConflict(
            RuntimeException ex
    ) {

        String message = ex instanceof ConflictException
                ? ex.getMessage()
                : "This item was modified by someone else. Reload and try again.";

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        new ErrorResponse(
                                message,
                                OffsetDateTime.now()
                        )
                );
    }

    @ExceptionHandler(
            Exception.class
    )
    public ResponseEntity<ErrorResponse>
    handleGeneric(
            Exception ex
    ) {

        log.error("Unhandled exception", ex);

        return ResponseEntity
                .status(
                        HttpStatus.INTERNAL_SERVER_ERROR
                )
                .body(
                        new ErrorResponse(
                                "Something went wrong",
                                OffsetDateTime.now()
                        )
                );
    }
}