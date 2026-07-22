package com.logshield.logshieldv2.exception;

/**
 * Base runtime exception for all LogShield v2 errors.
 *
 * Extends RuntimeException — unchecked — so Spring can intercept
 * it via @ControllerAdvice without forcing try-catch everywhere.
 */
public class LogShieldException extends RuntimeException {

    private final int httpStatus;

    public LogShieldException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() { return httpStatus; }
}