package com.logshield.logshieldv2.exception;

/**
 * Thrown when a log level is not INFO, WARN, or ERROR.
 * Maps to HTTP 400 Bad Request.
 */
public class InvalidLogLevelException extends LogShieldException {

    public InvalidLogLevelException(String level) {
        super("Invalid log level: '" + level
                + "'. Must be INFO, WARN, or ERROR.", 400);
    }
}