package com.logshield.logshieldv2.storage;

import com.logshield.logshieldv2.model.LogEntryResponse;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all disk I/O for LogShield v2.
 *
 * Annotated with @Component — Spring creates one instance and
 * injects it into LogShieldServiceImpl via constructor injection.
 *
 * Write-Through policy: every addLog() writes to disk before
 * returning — if JVM crashes the entry survives on disk.
 *
 * Single Responsibility — this class only reads and writes files.
 * No business logic, no algorithms, no HTTP knowledge.
 */
@Component
public class FileHandler {

    // Default log file path — relative to project root
    private static final String DEFAULT_LOG_PATH = "data/logs.txt";

    // -------------------------------------------------------------------------
    // appendLog(LogEntryResponse entry)
    //
    // Time Complexity : O(1) — one line written per call
    // Space Complexity: O(1) — fixed buffer size
    //
    // Opens writer in APPEND mode — existing entries are never overwritten.
    // Write-Through: called before cache update in addLog().
    // -------------------------------------------------------------------------

    /**
     * Appends one log entry to the persistence file in CSV format.
     * Creates the file and parent directories if they do not exist.
     *
     * @param entry the log entry to persist
     */
    public void appendLog(LogEntryResponse entry) {
        appendLog(entry, DEFAULT_LOG_PATH);
    }

    /**
     * Appends one log entry to the specified file path.
     *
     * @param entry    the log entry to persist
     * @param filePath target file path
     */
    public void appendLog(LogEntryResponse entry, String filePath) {
        // Ensure parent directory exists — create if missing
        File file = new File(filePath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        // true = append mode — never overwrites existing entries
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(file, true))) {
            writer.write(toCSV(entry));
            writer.newLine();
        } catch (IOException e) {
            System.err.println("[FileHandler] Failed to write log: "
                    + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // loadLogs() → List<LogEntryResponse>
    //
    // Time Complexity : O(n) — reads n lines from file
    // Space Complexity: O(n) — all entries loaded into list
    //
    // Called once on application startup to restore persisted state.
    // Returns empty list if file does not exist — not an error.
    // -------------------------------------------------------------------------

    /**
     * Loads all log entries from the persistence file.
     * Returns an empty list if the file does not exist yet.
     *
     * @return list of all persisted log entries
     */
    public List<LogEntryResponse> loadLogs() {
        return loadLogs(DEFAULT_LOG_PATH);
    }

    /**
     * Loads all log entries from the specified file path.
     *
     * @param filePath source file path
     * @return list of all persisted log entries
     */
    public List<LogEntryResponse> loadLogs(String filePath) {
        List<LogEntryResponse> entries = new ArrayList<>();
        File file = new File(filePath);

        // File not found is not an error — first run has no file yet
        if (!file.exists()) {
            System.out.println("[FileHandler] No persistence file found at '"
                    + filePath + "'. Starting fresh.");
            return entries;
        }

        try (BufferedReader reader = new BufferedReader(
                new FileReader(file))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) continue;

                LogEntryResponse entry = fromCSV(line, lineNumber);
                if (entry != null) entries.add(entry);
            }

            System.out.println("[FileHandler] Loaded "
                    + entries.size() + " entries from '"
                    + filePath + "'.");

        } catch (IOException e) {
            System.err.println("[FileHandler] Failed to load logs: "
                    + e.getMessage());
        }

        return entries;
    }

    // -------------------------------------------------------------------------
    // rewriteAll(List<LogEntryResponse> entries)
    //
    // Time Complexity : O(n) — writes n entries to file
    // Space Complexity: O(1) — single BufferedWriter session
    //
    // Used after deleteLog() — full rewrite required for flat file storage.
    // One file open for all entries — batch write pattern.
    // -------------------------------------------------------------------------

    /**
     * Rewrites the entire persistence file with the given entries.
     * Used after deletion — flat file has no random-access delete.
     *
     * @param entries the complete current list of log entries
     */
    public void rewriteAll(List<LogEntryResponse> entries) {
        rewriteAll(entries, DEFAULT_LOG_PATH);
    }

    /**
     * Rewrites the specified file with the given entries.
     *
     * @param entries  the complete current list of log entries
     * @param filePath target file path
     */
    public void rewriteAll(List<LogEntryResponse> entries, String filePath) {
        File file = new File(filePath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        // false = overwrite mode — replace entire file content
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(file, false))) {
            for (LogEntryResponse entry : entries) {
                writer.write(toCSV(entry));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("[FileHandler] Failed to rewrite logs: "
                    + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // clearFile()
    //
    // Time Complexity : O(1) — truncates file to zero bytes
    // -------------------------------------------------------------------------

    /**
     * Clears the persistence file without deleting it.
     * Used by clearAllLogs() to reset persisted state.
     */
    public void clearFile() {
        clearFile(DEFAULT_LOG_PATH);
    }

    public void clearFile(String filePath) {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(filePath, false))) {
            writer.write("");
        } catch (IOException e) {
            System.err.println("[FileHandler] Failed to clear file: "
                    + e.getMessage());
        }
    }

    // ── CSV helpers ───────────────────────────────────────────────────────────

    /**
     * Converts a LogEntryResponse to CSV format for file storage.
     * Format: timestamp,level,message,severityScore
     */
    private String toCSV(LogEntryResponse entry) {
        return entry.getTimestamp() + ","
                + entry.getLevel() + ","
                + entry.getMessage() + ","
                + entry.getSeverityScore();
    }

    /**
     * Parses a CSV line back into a LogEntryResponse.
     * Returns null and logs a warning if the line is malformed.
     * Split limit of 4 preserves commas inside message field.
     */
    private LogEntryResponse fromCSV(String line, int lineNumber) {
        String[] parts = line.split(",", 4);

        if (parts.length < 4) {
            System.err.println("[FileHandler] WARN: Skipping malformed"
                    + " line " + lineNumber + ": '" + line + "'");
            return null;
        }

        try {
            String timestamp    = parts[0].trim();
            String level        = parts[1].trim();
            String message      = parts[2].trim();
            int    severityScore = Integer.parseInt(parts[3].trim());

            return new LogEntryResponse(
                    timestamp, level, message, severityScore);

        } catch (NumberFormatException e) {
            System.err.println("[FileHandler] WARN: Invalid severity"
                    + " score on line " + lineNumber + ": '" + line + "'");
            return null;
        }
    }
}