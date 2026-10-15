package com.tripnext.demo.exception;

public class ConflictException
        extends RuntimeException {

    public ConflictException(
            String message
    ) {
        super(message);
    }
}
