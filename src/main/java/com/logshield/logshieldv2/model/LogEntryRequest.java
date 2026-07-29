package com.logshield.logshieldv2.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO representing the JSON body a client sends when adding a log entry.
 *
 * Uses Bean Validation annotations — validation happens automatically
 * before the controller method is called. No manual null checks needed.
 *
 * @NotBlank — rejects null, empty, and whitespace-only strings
 * @Pattern  — enforces allowed values using regex
 * @Size     — enforces length limits
 */
public class LogEntryRequest {

    // Timestamp cannot be blank — it is the unique key in the cache
    // Pattern enforces ISO format: YYYY-MM-DD HH:MM:SS
    @NotBlank(message = "Timestamp is required")
    @Pattern(
            regexp = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}",
            message = "Timestamp must be in format: YYYY-MM-DD HH:MM:SS"
    )
    private String timestamp;

    // Level must be exactly INFO, WARN, or ERROR — case insensitive handled in service
    @NotBlank(message = "Level is required")
    @Pattern(
            regexp = "(?i)INFO|WARN|ERROR",
            message = "Level must be INFO, WARN, or ERROR"
    )
    private String level;

    // Message cannot be blank and must be under 500 characters
    @NotBlank(message = "Message is required")
    @Size(
            min = 1,
            max = 500,
            message = "Message must be between 1 and 500 characters"
    )
    private String message;

    // No-arg constructor required by Jackson for JSON deserialization
    public LogEntryRequest() {}

    public LogEntryRequest(String timestamp, String level, String message) {
        this.timestamp = timestamp;
        this.level     = level;
        this.message   = message;
    }

    public String getTimestamp() { return timestamp; }
    public String getLevel()     { return level; }
    public String getMessage()   { return message; }

    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public void setLevel(String level)         { this.level = level; }
    public void setMessage(String message)     { this.message = message; }
}