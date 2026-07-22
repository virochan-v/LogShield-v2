package com.logshield.logshieldv2.model;

/**
 * DTO representing what the API returns for a single log entry.
 *
 * Decouples the API response from the internal LogEntry domain model.
 * Adding a new response field here never changes the domain model.
 */
public class LogEntryResponse {

    private String timestamp;
    private String level;
    private String message;
    private int    severityScore;

    // No-arg constructor required by Jackson for JSON serialization
    public LogEntryResponse() {}

    public LogEntryResponse(String timestamp, String level,
                            String message, int severityScore) {
        this.timestamp     = timestamp;
        this.level         = level;
        this.message       = message;
        this.severityScore = severityScore;
    }

    public String getTimestamp()    { return timestamp; }
    public String getLevel()        { return level; }
    public String getMessage()      { return message; }
    public int    getSeverityScore(){ return severityScore; }

    public void setTimestamp(String timestamp)       { this.timestamp = timestamp; }
    public void setLevel(String level)               { this.level = level; }
    public void setMessage(String message)           { this.message = message; }
    public void setSeverityScore(int severityScore)  { this.severityScore = severityScore; }
}