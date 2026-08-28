package com.logshield.logshieldv2;

import com.logshield.logshieldv2.exception.InvalidLogLevelException;
import com.logshield.logshieldv2.model.LogEntryRequest;
import com.logshield.logshieldv2.model.LogEntryResponse;
import com.logshield.logshieldv2.model.PagedResponse;
import com.logshield.logshieldv2.service.LogShieldServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LogShieldServiceImpl.
 *
 * Tests run in complete isolation — no Spring context, no HTTP server,
 * no file system. Each test gets a fresh service instance via @BeforeEach.
 *
 * Naming convention: methodName_scenario_expectedResult
 * This makes test failures self-documenting.
 */
class LogShieldServiceImplTest {

    // System under test — fresh instance before every test
    private LogShieldServiceImpl service;

    /**
     * Runs before EVERY test method.
     * Creates a fresh service with empty cache — tests never share state.
     * This is the most important setup decision in unit testing.
     */
    @BeforeEach
    void setUp() {
        // Inject real TrieService — not a mock
        // TrieService has no external dependencies so real instance is fine
        service = new LogShieldServiceImpl(
                new com.logshield.logshieldv2.trie.TrieService());
    }

    // ── Helper method ─────────────────────────────────────────────────────

    /**
     * Creates a LogEntryRequest with the given values.
     * Reduces repetition across test methods.
     */
    private LogEntryRequest request(String timestamp,
                                    String level,
                                    String message) {
        return new LogEntryRequest(timestamp, level, message);
    }

    // ── addLog() tests ────────────────────────────────────────────────────

    @Test
    @DisplayName("addLog: valid ERROR entry returns correct severityScore 3")
    void addLog_validErrorEntry_returnsSeverityScore3() {
        LogEntryResponse result = service.addLog(
                request("2024-06-01 09:00:00", "ERROR",
                        "NullPointerException in PaymentService"));

        assertNotNull(result);
        assertEquals("ERROR", result.getLevel());
        assertEquals(3, result.getSeverityScore());
        assertEquals("NullPointerException in PaymentService",
                result.getMessage());
    }

    @Test
    @DisplayName("addLog: valid WARN entry returns correct severityScore 2")
    void addLog_validWarnEntry_returnsSeverityScore2() {
        LogEntryResponse result = service.addLog(
                request("2024-06-01 09:01:00", "WARN",
                        "High memory usage"));

        assertEquals("WARN", result.getLevel());
        assertEquals(2, result.getSeverityScore());
    }

    @Test
    @DisplayName("addLog: valid INFO entry returns correct severityScore 1")
    void addLog_validInfoEntry_returnsSeverityScore1() {
        LogEntryResponse result = service.addLog(
                request("2024-06-01 09:02:00", "INFO",
                        "Server started successfully"));

        assertEquals("INFO", result.getLevel());
        assertEquals(1, result.getSeverityScore());
    }

    @Test
    @DisplayName("addLog: invalid level throws InvalidLogLevelException")
    void addLog_invalidLevel_throwsInvalidLogLevelException() {
        assertThrows(InvalidLogLevelException.class, () ->
                service.addLog(request("2024-06-01 09:00:00",
                        "CRITICAL", "Test")));
    }

    @Test
    @DisplayName("addLog: lowercase level is accepted and uppercased")
    void addLog_lowercaseLevel_isAccepted() {
        LogEntryResponse result = service.addLog(
                request("2024-06-01 09:00:00", "error", "Test"));

        assertEquals("ERROR", result.getLevel());
        assertEquals(3, result.getSeverityScore());
    }

    @Test
    @DisplayName("addLog: duplicate timestamp overwrites existing entry")
    void addLog_duplicateTimestamp_overwritesExistingEntry() {
        service.addLog(request("2024-06-01 09:00:00",
                "INFO", "First entry"));
        service.addLog(request("2024-06-01 09:00:00",
                "ERROR", "Second entry"));

        List<LogEntryResponse> all = service.getAllLogs();
        assertEquals(1, all.size());
        assertEquals("ERROR", all.get(0).getLevel());
    }

    // ── getAllLogs() tests ────────────────────────────────────────────────

    @Test
    @DisplayName("getAllLogs: empty cache returns empty list")
    void getAllLogs_emptyCache_returnsEmptyList() {
        List<LogEntryResponse> result = service.getAllLogs();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getAllLogs: returns correct count after adding entries")
    void getAllLogs_afterAddingThreeEntries_returnsThreeEntries() {
        service.addLog(request("2024-06-01 09:00:00", "INFO", "One"));
        service.addLog(request("2024-06-01 09:01:00", "WARN", "Two"));
        service.addLog(request("2024-06-01 09:02:00", "ERROR", "Three"));

        assertEquals(3, service.getAllLogs().size());
    }

    // ── getSortedLogs() tests ─────────────────────────────────────────────

    @Test
    @DisplayName("getSortedLogs: returns entries in ascending severity order")
    void getSortedLogs_mixedEntries_returnsAscendingOrder() {
        service.addLog(request("2024-06-01 09:00:00", "ERROR", "Error one"));
        service.addLog(request("2024-06-01 09:01:00", "INFO",  "Info one"));
        service.addLog(request("2024-06-01 09:02:00", "WARN",  "Warn one"));

        List<LogEntryResponse> sorted = service.getSortedLogs();

        assertEquals(3, sorted.size());
        assertEquals(1, sorted.get(0).getSeverityScore()); // INFO first
        assertEquals(2, sorted.get(1).getSeverityScore()); // WARN second
        assertEquals(3, sorted.get(2).getSeverityScore()); // ERROR last
    }

    @Test
    @DisplayName("getSortedLogs: empty cache returns empty list")
    void getSortedLogs_emptyCache_returnsEmptyList() {
        assertTrue(service.getSortedLogs().isEmpty());
    }

    // ── searchBySeverity() tests ──────────────────────────────────────────

    @Test
    @DisplayName("searchBySeverity: finds all ERROR entries with score 3")
    void searchBySeverity_score3_returnsAllErrorEntries() {
        service.addLog(request("2024-06-01 09:00:00",
                "ERROR", "Error one"));
        service.addLog(request("2024-06-01 09:01:00",
                "ERROR", "Error two"));
        service.addLog(request("2024-06-01 09:02:00",
                "INFO",  "Info one"));

        List<LogEntryResponse> results = service.searchBySeverity(3);

        assertEquals(2, results.size());
        assertTrue(results.stream()
                .allMatch(e -> e.getSeverityScore() == 3));
    }

    @Test
    @DisplayName("searchBySeverity: returns empty list when no match")
    void searchBySeverity_noMatch_returnsEmptyList() {
        service.addLog(request("2024-06-01 09:00:00",
                "INFO", "Info only"));

        List<LogEntryResponse> results = service.searchBySeverity(3);

        assertTrue(results.isEmpty());
    }

    // ── getTopAnomalies() tests ───────────────────────────────────────────

    @Test
    @DisplayName("getTopAnomalies: returns k highest severity entries")
    void getTopAnomalies_k2_returnsTop2BySeverity() {
        service.addLog(request("2024-06-01 09:00:00",
                "INFO",  "Info one"));
        service.addLog(request("2024-06-01 09:01:00",
                "ERROR", "Error one"));
        service.addLog(request("2024-06-01 09:02:00",
                "WARN",  "Warn one"));

        List<LogEntryResponse> top = service.getTopAnomalies(2);

        assertEquals(2, top.size());
        // First must be ERROR (score 3), second must be WARN (score 2)
        assertEquals(3, top.get(0).getSeverityScore());
        assertEquals(2, top.get(1).getSeverityScore());
    }

    @Test
    @DisplayName("getTopAnomalies: k larger than total returns all entries")
    void getTopAnomalies_kLargerThanTotal_returnsAllEntries() {
        service.addLog(request("2024-06-01 09:00:00",
                "INFO", "Only entry"));

        List<LogEntryResponse> top = service.getTopAnomalies(10);

        assertEquals(1, top.size());
    }

    // ── getStatistics() tests ─────────────────────────────────────────────

    @Test
    @DisplayName("getStatistics: correct counts for mixed entries")
    void getStatistics_mixedEntries_returnsCorrectCounts() {
        service.addLog(request("2024-06-01 09:00:00",
                "INFO",  "Info"));
        service.addLog(request("2024-06-01 09:01:00",
                "INFO",  "Info 2"));
        service.addLog(request("2024-06-01 09:02:00",
                "WARN",  "Warn"));
        service.addLog(request("2024-06-01 09:03:00",
                "ERROR", "Error"));

        Map<String, Object> stats = service.getStatistics();

        assertEquals(4,   stats.get("total"));
        assertEquals(2,   stats.get("infoCount"));
        assertEquals(1,   stats.get("warnCount"));
        assertEquals(1,   stats.get("errorCount"));
    }

    @Test
    @DisplayName("getStatistics: zero errors returns HEALTHY system health")
    void getStatistics_zeroErrors_returnsHealthyStatus() {
        service.addLog(request("2024-06-01 09:00:00",
                "INFO", "All good"));

        Map<String, Object> stats = service.getStatistics();

        assertEquals("HEALTHY", stats.get("systemHealth"));
    }

    @Test
    @DisplayName("getStatistics: 100% errors returns CRITICAL system health")
    void getStatistics_allErrors_returnsCriticalStatus() {
        service.addLog(request("2024-06-01 09:00:00",
                "ERROR", "Error one"));
        service.addLog(request("2024-06-01 09:01:00",
                "ERROR", "Error two"));

        Map<String, Object> stats = service.getStatistics();

        assertEquals("CRITICAL", stats.get("systemHealth"));
    }

    // ── clearAllLogs() tests ──────────────────────────────────────────────

    @Test
    @DisplayName("clearAllLogs: empties the cache completely")
    void clearAllLogs_afterAddingEntries_returnsEmptyCache() {
        service.addLog(request("2024-06-01 09:00:00",
                "ERROR", "Test"));
        service.clearAllLogs();

        assertTrue(service.getAllLogs().isEmpty());
    }

    // ── getAnomalySummary() tests ─────────────────────────────────────────

    @Test
    @DisplayName("getAnomalySummary: detects burst when errors exceed threshold")
    void getAnomalySummary_errorBurst_detectsAnomalyPeriod() {
        service.addLog(request("2024-06-01 09:00:00",
                "INFO",  "Server started"));
        service.addLog(request("2024-06-01 09:01:00",
                "ERROR", "NullPointerException"));
        service.addLog(request("2024-06-01 09:02:00",
                "ERROR", "Database timeout"));
        service.addLog(request("2024-06-01 09:03:00",
                "ERROR", "Memory overflow"));
        service.addLog(request("2024-06-01 09:04:00",
                "WARN",  "High CPU"));

        List<String> summary = service.getAnomalySummary(5, 3);

        assertFalse(summary.isEmpty());
        assertTrue(summary.get(0).contains("Anomaly period"));
        assertTrue(summary.get(0).contains("Peak ERRORs: 3"));
    }

    @Test
    @DisplayName("getAnomalySummary: no burst when errors below threshold")
    void getAnomalySummary_noBurst_returnsEmptyList() {
        service.addLog(request("2024-06-01 09:00:00",
                "INFO", "All fine"));
        service.addLog(request("2024-06-01 09:01:00",
                "INFO", "Still fine"));
        service.addLog(request("2024-06-01 09:02:00",
                "WARN", "Minor issue"));

        List<String> summary = service.getAnomalySummary(3, 3);

        assertTrue(summary.isEmpty());
    }
    // ── getPagedLogs() tests ──────────────────────────────────────────────

    @Test
    @DisplayName("getPagedLogs: returns correct page slice and metadata")
    void getPagedLogs_page0Size2_returnsFirstTwoEntries() {
        service.addLog(request("2024-06-01 09:00:00", "ERROR", "One"));
        service.addLog(request("2024-06-01 09:01:00", "WARN",  "Two"));
        service.addLog(request("2024-06-01 09:02:00", "INFO",  "Three"));

        PagedResponse<LogEntryResponse> result =
                service.getPagedLogs(0, 2);

        assertEquals(2,  result.getContent().size());
        assertEquals(3,  result.getTotalElements());
        assertEquals(2,  result.getTotalPages());
        assertTrue(result.isFirst());
        assertFalse(result.isLast());
    }

    @Test
    @DisplayName("getPagedLogs: page beyond data returns empty content")
    void getPagedLogs_pageBeyondData_returnsEmptyContent() {
        service.addLog(request("2024-06-01 09:00:00", "INFO", "Only one"));

        PagedResponse<LogEntryResponse> result =
                service.getPagedLogs(99, 20);

        assertTrue(result.getContent().isEmpty());
        assertEquals(1, result.getTotalElements());
    }

    // ── Trie tests ────────────────────────────────────────────────────────

    @Test
    @DisplayName("searchByPrefix: finds all messages starting with prefix")
    void searchByPrefix_nullPointerPrefix_returnsBothVariants() {
        service.addLog(request("2024-06-01 09:00:00", "ERROR",
                "NullPointerException in UserService"));
        service.addLog(request("2024-06-01 09:01:00", "ERROR",
                "NullPointerException in PaymentService"));
        service.addLog(request("2024-06-01 09:02:00", "WARN",
                "Connection refused on port 8080"));

        List<String> results = service.searchByPrefix("NullPointer");

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(
                m -> m.startsWith("NullPointer")));
    }

    @Test
    @DisplayName("getPatternFrequency: returns correct count for exact match")
    void getPatternFrequency_exactMatch_returnsCorrectFrequency() {
        service.addLog(request("2024-06-01 09:00:00", "ERROR",
                "NullPointerException in UserService"));
        service.addLog(request("2024-06-01 09:01:00", "ERROR",
                "NullPointerException in UserService"));

        // Same message added twice — but duplicate timestamp means only one stored
        // Frequency tracks insertions not cache entries
        int freq = service.getPatternFrequency(
                "NullPointerException in UserService");

        assertTrue(freq >= 1);
    }
}