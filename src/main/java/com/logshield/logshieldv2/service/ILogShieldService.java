package com.logshield.logshieldv2.service;

import com.logshield.logshieldv2.model.LogEntryRequest;
import com.logshield.logshieldv2.model.LogEntryResponse;

import java.util.List;
import java.util.Map;

/**
 * Contract for LogShield v2 business logic layer.
 *
 * Controllers depend on this interface — not on the concrete
 * implementation. Satisfies DIP — high-level modules depend
 * on abstractions, not concretions.
 *
 * Swapping LogShieldServiceImpl for a database-backed implementation
 * requires zero changes in LogShieldController.
 */
public interface ILogShieldService {

    // Add a single log entry — returns the created entry
    LogEntryResponse addLog(LogEntryRequest request);

    // Get all logs in current in-memory state
    List<LogEntryResponse> getAllLogs();

    // Sort logs by severity and return sorted list
    List<LogEntryResponse> getSortedLogs();

    // Search all logs matching a severity score
    List<LogEntryResponse> searchBySeverity(int score);

    // Get top k anomalies by severity using MinHeap
    List<LogEntryResponse> getTopAnomalies(int k);

    // Search exact pattern in Trie
    int getPatternFrequency(String pattern);

    // Search all patterns starting with prefix
    List<String> searchByPrefix(String prefix);

    // Get log statistics — total, INFO count, WARN count, ERROR count
    Map<String, Object> getStatistics();

    // Filter logs by time range
    List<LogEntryResponse> filterByTimeRange(String start, String end);

    // Detect anomaly bursts using sliding window
    List<String> getAnomalySummary(int windowSize, int errorThreshold);

    // Delete log entry by timestamp — returns true if deleted
    boolean deleteLog(String timestamp);

    // Clear all logs
    void clearAllLogs();

    // Export logs to file
    void exportLogs(String filePath);
}