package com.logshield.logshieldv2.exception;

/**
 * Thrown when a requested log entry does not exist.
 * Maps to HTTP 404 Not Found.
 */
public class LogNotFoundException extends LogShieldException {

    public LogNotFoundException(String timestamp) {
        super("No log entry found with timestamp: '" + timestamp + "'", 404);
    }
}