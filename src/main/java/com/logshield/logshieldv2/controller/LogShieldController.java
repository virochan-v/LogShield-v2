package com.logshield.logshieldv2.controller;

import com.logshield.logshieldv2.model.ApiResponse;
import com.logshield.logshieldv2.model.LogEntryRequest;
import com.logshield.logshieldv2.model.LogEntryResponse;
import com.logshield.logshieldv2.service.ILogShieldService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for LogShield v2 API.
 *
 * Single responsibility — handles HTTP requests and responses only.
 * Never contains business logic — delegates everything to ILogShieldService.
 *
 * Depends on ILogShieldService interface — not the concrete implementation.
 * This satisfies DIP — swapping implementations requires zero changes here.
 *
 * All endpoints are versioned under /api/v1 — allows future v2 endpoints
 * without breaking existing clients.
 */
@RestController
@RequestMapping("/api/v1/logs")
public class LogShieldController {

    // Depends on abstraction — not LogShieldServiceImpl directly
    // Spring injects the correct implementation automatically via @Service
    private final ILogShieldService service;

    // Constructor injection — preferred over @Autowired field injection
    // Makes dependencies explicit and enables easier unit testing
    public LogShieldController(ILogShieldService service) {
        this.service = service;
    }

    // ── CREATE ────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/logs
     * Add a new log entry.
     *
     * Request body:
     * {
     *   "timestamp": "2024-06-01 09:00:00",
     *   "level": "ERROR",
     *   "message": "NullPointerException in PaymentService"
     * }
     *
     * Returns 201 Created on success.
     * Returns 400 Bad Request if level is invalid or fields are blank.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<LogEntryResponse>> addLog(
            @RequestBody LogEntryRequest request) {

        LogEntryResponse entry = service.addLog(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Log entry added successfully.", entry));
    }

    // ── READ ──────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/logs
     * Get all log entries in current in-memory state.
     * Returns 200 OK with list of all entries.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<LogEntryResponse>>> getAllLogs() {

        List<LogEntryResponse> logs = service.getAllLogs();
        return ResponseEntity.ok(
                ApiResponse.success(
                        logs.size() + " log entries retrieved.", logs));
    }

    /**
     * GET /api/v1/logs/sorted
     * Sort logs by severity score ascending (INFO → WARN → ERROR).
     * Uses Cycle Sort — O(n²) time, O(1) space, minimum memory writes.
     */
    @GetMapping("/sorted")
    public ResponseEntity<ApiResponse<List<LogEntryResponse>>> getSortedLogs() {

        List<LogEntryResponse> sorted = service.getSortedLogs();
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Logs sorted by severity (Cycle Sort — O(n²)).",
                        sorted));
    }

    /**
     * GET /api/v1/logs/search/{score}
     * Search all log entries matching the given severity score.
     * Uses Binary Search + expand — O(log n + d).
     *
     * Path variable:
     * score = 1 (INFO), 2 (WARN), 3 (ERROR)
     */
    @GetMapping("/search/{score}")
    public ResponseEntity<ApiResponse<List<LogEntryResponse>>> searchBySeverity(
            @PathVariable int score) {

        List<LogEntryResponse> results = service.searchBySeverity(score);
        return ResponseEntity.ok(
                ApiResponse.success(
                        results.size() + " log(s) found with severity "
                                + score + " — O(log n + d).",
                        results));
    }

    /**
     * GET /api/v1/logs/top/{k}
     * Get top k anomalies by severity using MinHeap.
     * Space complexity O(k) regardless of total entries.
     */
    @GetMapping("/top/{k}")
    public ResponseEntity<ApiResponse<List<LogEntryResponse>>> getTopAnomalies(
            @PathVariable int k) {

        List<LogEntryResponse> top = service.getTopAnomalies(k);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Top " + k + " anomalies retrieved (MinHeap — O(n log k)).",
                        top));
    }

    /**
     * GET /api/v1/logs/pattern?query=NullPointerException
     * Search exact pattern frequency in log messages.
     */
    @GetMapping("/pattern")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPatternFrequency(
            @RequestParam String query) {

        int freq = service.getPatternFrequency(query);
        Map<String, Object> result = Map.of(
                "pattern",   query,
                "frequency", freq,
                "found",     freq > 0
        );
        return ResponseEntity.ok(
                ApiResponse.success("Pattern search complete.", result));
    }

    /**
     * GET /api/v1/logs/prefix?query=NullPointer
     * Search all log messages starting with the given prefix.
     * Returns all matching message strings.
     */
    @GetMapping("/prefix")
    public ResponseEntity<ApiResponse<List<String>>> searchByPrefix(
            @RequestParam String query) {

        List<String> matches = service.searchByPrefix(query);
        return ResponseEntity.ok(
                ApiResponse.success(
                        matches.size() + " message(s) starting with '"
                                + query + "'.",
                        matches));
    }

    /**
     * GET /api/v1/logs/statistics
     * Returns log breakdown with health indicator.
     * Single O(n) pass — three counters only.
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {

        Map<String, Object> stats = service.getStatistics();
        return ResponseEntity.ok(
                ApiResponse.success("Statistics retrieved.", stats));
    }

    /**
     * GET /api/v1/logs/range?start=2024-01-01 00:00:00&end=2024-12-31 23:59:59
     * Filter logs within a timestamp range.
     * ISO format timestamps sort lexicographically = chronologically.
     */
    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<LogEntryResponse>>> filterByTimeRange(
            @RequestParam String start,
            @RequestParam String end) {

        List<LogEntryResponse> results =
                service.filterByTimeRange(start, end);
        return ResponseEntity.ok(
                ApiResponse.success(
                        results.size() + " log(s) in range "
                                + start + " → " + end + ".",
                        results));
    }

    /**
     * GET /api/v1/logs/anomalies?windowSize=5&errorThreshold=3
     * Detect anomaly bursts using Sliding Window algorithm.
     * O(n) detection after O(n log n) timestamp sort.
     */
    @GetMapping("/anomalies")
    public ResponseEntity<ApiResponse<List<String>>> getAnomalySummary(
            @RequestParam(defaultValue = "5")  int windowSize,
            @RequestParam(defaultValue = "3")  int errorThreshold) {

        List<String> summary =
                service.getAnomalySummary(windowSize, errorThreshold);
        return ResponseEntity.ok(
                ApiResponse.success(
                        summary.isEmpty()
                                ? "No anomaly bursts detected. System stable."
                                : summary.size() + " anomaly period(s) detected.",
                        summary));
    }

    // ── DELETE ────────────────────────────────────────────────────────────

    /**
     * DELETE /api/v1/logs/{timestamp}
     * Delete a specific log entry by exact timestamp.
     * Throws LogNotFoundException if timestamp does not exist.
     */
    @DeleteMapping("/{timestamp}")
    public ResponseEntity<ApiResponse<Void>> deleteLog(
            @PathVariable String timestamp) {

        service.deleteLog(timestamp);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Log entry deleted: '" + timestamp + "'", null));
    }

    /**
     * DELETE /api/v1/logs
     * Clear all log entries from memory.
     * Returns 200 OK with confirmation.
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearAllLogs() {

        service.clearAllLogs();
        return ResponseEntity.ok(
                ApiResponse.success("All log entries cleared.", null));
    }
}