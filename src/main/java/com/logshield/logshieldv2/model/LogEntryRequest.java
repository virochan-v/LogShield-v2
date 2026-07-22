package com.logshield.logshieldv2.model;

/**
 * DTO representing the JSON body a client sends when adding a log entry.
 *
 * Keeps the API contract separate from the internal LogEntry domain model.
 * The client never sees persistence methods like toCSV() or fromCSV().
 *
 * OCP compliant — adding new fields here never changes controller or service.
 */
public class LogEntryRequest {

    // Three fields the client must provide — matches LogEntry constructor
    private String timestamp;
    private String level;
    private String message;

    // No-arg constructor required by Jackson for JSON deserialization
    // Jackson uses this to create the object before setting fields
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