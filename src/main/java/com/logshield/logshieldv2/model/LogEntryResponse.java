package com.logshield.logshieldv2.model;

/**
 * DTO representing what the API returns for a single log entry.
 *
 * Now includes UUID id — clients use this to reference specific
 * entries for deletion or update operations.
 *
 * Decouples the API response from JPA entity — controller never
 * sees @Entity, @Column or other persistence annotations.
 */
public class LogEntryResponse {

    // UUID as String — client friendly, no UUID import needed on client
    private String id;
    private String timestamp;
    private String level;
    private String message;
    private int    severityScore;

    public LogEntryResponse() {}

    public LogEntryResponse(String id, String timestamp,
                            String level, String message,
                            int severityScore) {
        this.id            = id;
        this.timestamp     = timestamp;
        this.level         = level;
        this.message       = message;
        this.severityScore = severityScore;
    }

    // Keep backward-compatible constructor for tests that don't use id
    public LogEntryResponse(String timestamp, String level,
                            String message, int severityScore) {
        this.id            = null;
        this.timestamp     = timestamp;
        this.level         = level;
        this.message       = message;
        this.severityScore = severityScore;
    }

    public String getId()           { return id; }
    public String getTimestamp()    { return timestamp; }
    public String getLevel()        { return level; }
    public String getMessage()      { return message; }
    public int getSeverityScore()   { return severityScore; }

    public void setId(String id)                      { this.id = id; }
    public void setTimestamp(String timestamp)         { this.timestamp = timestamp; }
    public void setLevel(String level)                 { this.level = level; }
    public void setMessage(String message)             { this.message = message; }
    public void setSeverityScore(int severityScore)    { this.severityScore = severityScore; }
}