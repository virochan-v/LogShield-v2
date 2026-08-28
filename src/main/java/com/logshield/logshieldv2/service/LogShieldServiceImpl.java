package com.logshield.logshieldv2.service;

import com.logshield.logshieldv2.exception.InvalidLogLevelException;
import com.logshield.logshieldv2.exception.LogNotFoundException;
import com.logshield.logshieldv2.model.LogEntryRequest;
import com.logshield.logshieldv2.model.LogEntryResponse;
import com.logshield.logshieldv2.model.PagedResponse;
import com.logshield.logshieldv2.trie.TrieService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core business logic implementation for LogShield v2.
 *
 * Annotated with @Service — Spring creates exactly one instance
 * and manages its lifecycle. This replaces the manual Singleton
 * pattern from v1. Spring's IoC container IS the Singleton manager.
 *
 * Uses ConcurrentHashMap instead of HashMap — thread-safe for
 * concurrent HTTP requests. Multiple requests can hit the server
 * simultaneously unlike the CLI which handled one input at a time.
 *
 * All v1 algorithms are reimplemented here:
 * - Cycle Sort for severity ranking
 * - Binary Search for O(log n) retrieval
 * - MinHeap for top-k anomalies
 * - Sliding Window for burst detection
 * - Trie for pattern matching
 */
@Service
public class LogShieldServiceImpl implements ILogShieldService {

    // ── Constants ─────────────────────────────────────────────────────────

    // OCP compliant — adding CRITICAL requires one Map entry, zero code changes
    private static final Map<String, Integer> SEVERITY_MAP = Map.of(
            "ERROR", 3,
            "WARN",  2,
            "INFO",  1
    );

    // ── In-memory storage ─────────────────────────────────────────────────

    // ConcurrentHashMap — thread-safe for simultaneous HTTP requests
    // Key = timestamp (unique identifier per log entry)
    private final Map<String, LogEntryResponse> logCache
            = new ConcurrentHashMap<>();

    // ── Helper: validate and map ──────────────────────────────────────────

    // TrieService — injected via constructor, owns all pattern tracking
    private final TrieService trieService;

    // Constructor injection — TrieService injected by Spring
    // Preferred over @Autowired field injection — explicit and testable
    public LogShieldServiceImpl(TrieService trieService) {
        this.trieService = trieService;
    }

    /**
     * Validates the log level and returns severity score.
     * Throws InvalidLogLevelException for unrecognised levels.
     * This replaces the constructor-level validation from v1.
     */
    private int resolveSeverity(String level) {
        Integer score = SEVERITY_MAP.get(level.toUpperCase());
        if (score == null) {
            throw new InvalidLogLevelException(level);
        }
        return score;
    }

    /**
     * Converts a LogEntryResponse to a sortable array element.
     * Used internally by sort and search operations.
     */
    private LogEntryResponse[] getCacheAsArray() {
        return logCache.values().toArray(new LogEntryResponse[0]);
    }

    // ── CRUD Operations ───────────────────────────────────────────────────

    /**
     * Adds a log entry to the in-memory cache.
     * Validates level, computes severity, stores entry.
     *
     * Time Complexity: O(1) average — ConcurrentHashMap.put()
     * Space Complexity: O(1) — one entry added
     */
    @Override
    public LogEntryResponse addLog(LogEntryRequest request) {

        // Resolve severity — throws if level is invalid
        String level = request.getLevel().toUpperCase().trim();
        int severityScore = resolveSeverity(level);

        // Build response object — this is what gets stored and returned
        LogEntryResponse entry = new LogEntryResponse(
                request.getTimestamp().trim(),
                level,
                request.getMessage().trim(),
                severityScore
        );

        logCache.put(entry.getTimestamp(), entry);

        // Track pattern in Trie — O(L) insert or frequency increment
        trieService.trackPattern(entry.getMessage());

        return entry;
    }

    /**
     * Returns all log entries as a list.
     * Order is not guaranteed — HashMap has no natural order.
     *
     * Time Complexity: O(n)
     * Space Complexity: O(n)
     */
    @Override
    public List<LogEntryResponse> getAllLogs() {
        return new ArrayList<>(logCache.values());
    }

    /**
     * Sorts log entries by severity score using Cycle Sort.
     * Returns sorted list — ascending severity (INFO → WARN → ERROR).
     *
     * Time Complexity: O(n²) — Cycle Sort
     * Space Complexity: O(1) extra — in-place sort
     */

    /**
     * Returns a paginated subset of log entries.
     *
     * Retrieves all entries, then applies offset and limit.
     * For large datasets a database with SQL LIMIT/OFFSET would be used —
     * this in-memory implementation demonstrates the pagination contract.
     *
     * Time Complexity : O(n) — full collection scan then subList
     * Space Complexity: O(size) — only one page held in response
     *
     * @param page zero-based page number
     * @param size number of entries per page
     * @return PagedResponse containing the requested page and metadata
     */
    @Override
    public PagedResponse<LogEntryResponse> getPagedLogs(int page, int size) {

        // Guard: enforce sensible defaults
        if (page < 0)  page = 0;
        if (size <= 0) size = 20;
        if (size > 100) size = 100; // cap at 100 — prevent abuse

        List<LogEntryResponse> all = new ArrayList<>(logCache.values());
        long totalElements = all.size();

        // Calculate start and end index for this page
        int fromIndex = page * size;
        int toIndex   = (int) Math.min(fromIndex + size, totalElements);

        // Handle page beyond available data
        if (fromIndex >= totalElements) {
            return new PagedResponse<>(
                    new ArrayList<>(), page, size, totalElements);
        }

        // Extract the page slice
        List<LogEntryResponse> pageContent = all.subList(fromIndex, toIndex);

        return new PagedResponse<>(pageContent, page, size, totalElements);
    }

    @Override
    public List<LogEntryResponse> getSortedLogs() {
        LogEntryResponse[] arr = getCacheAsArray();
        cycleSort(arr);
        return Arrays.asList(arr);
    }

    /**
     * Searches all log entries matching the given severity score.
     * Uses Binary Search after sorting — O(log n + d).
     *
     * Time Complexity: O(n²) sort + O(log n + d) search
     * Space Complexity: O(d) — d = matching entries
     */
    @Override
    public List<LogEntryResponse> searchBySeverity(int score) {
        LogEntryResponse[] arr = getCacheAsArray();
        cycleSort(arr);
        return searchAllBySeverity(arr, score);
    }

    /**
     * Returns top k entries by severity using a MinHeap.
     * Space complexity O(k) regardless of total entries.
     *
     * Time Complexity: O(n log k)
     * Space Complexity: O(k)
     */
    @Override
    public List<LogEntryResponse> getTopAnomalies(int k) {
        // MinHeap — smallest severity at top
        PriorityQueue<LogEntryResponse> minHeap = new PriorityQueue<>(
                Math.max(1, k),
                Comparator.comparingInt(LogEntryResponse::getSeverityScore)
        );

        for (LogEntryResponse entry : logCache.values()) {
            if (minHeap.size() < k) {
                minHeap.add(entry);
            } else if (entry.getSeverityScore()
                    > minHeap.peek().getSeverityScore()) {
                minHeap.poll();
                minHeap.add(entry);
            }
        }

        // Sort result descending for display
        List<LogEntryResponse> result = new ArrayList<>(minHeap);
        result.sort((a, b) -> b.getSeverityScore() - a.getSeverityScore());
        return result;
    }

    /**
     * Returns frequency of exact pattern match using Trie.
     * Time Complexity: O(L) — replaces O(n) linear scan
     */
    @Override
    public int getPatternFrequency(String pattern) {
        return trieService.getFrequency(pattern);
    }

    /**
     * Returns all patterns starting with prefix using Trie DFS.
     * Time Complexity: O(L + W) — replaces O(n × L) linear scan
     */
    @Override
    public List<String> searchByPrefix(String prefix) {
        return trieService.searchByPrefix(prefix);
    }

    /**
     * Returns statistics about current log entries.
     * Single O(n) pass — three counters only.
     *
     * Time Complexity: O(n)
     * Space Complexity: O(1)
     */
    @Override
    public Map<String, Object> getStatistics() {
        int total = 0, info = 0, warn = 0, error = 0;

        for (LogEntryResponse entry : logCache.values()) {
            total++;
            switch (entry.getLevel()) {
                case "INFO":  info++;  break;
                case "WARN":  warn++;  break;
                case "ERROR": error++; break;
            }
        }

        double errorPercent = total > 0 ? (error * 100.0) / total : 0;
        String health = errorPercent == 0 ? "HEALTHY"
                : errorPercent <= 10      ? "DEGRADED"
                : "CRITICAL";

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total",        total);
        stats.put("infoCount",    info);
        stats.put("warnCount",    warn);
        stats.put("errorCount",   error);
        stats.put("errorPercent", String.format("%.1f%%", errorPercent));
        stats.put("systemHealth", health);
        return stats;
    }

    /**
     * Filters log entries whose timestamps fall within the given range.
     * Sorts by timestamp string — ISO format guarantees lexicographic
     * order equals chronological order.
     *
     * Time Complexity: O(n²) sort + O(n) scan
     * Space Complexity: O(n + m) — m = entries in range
     */
    @Override
    public List<LogEntryResponse> filterByTimeRange(
            String start, String end) {

        LogEntryResponse[] arr = getCacheAsArray();

        // Sort by timestamp string — lexicographic = chronological for ISO
        Arrays.sort(arr, Comparator.comparing(LogEntryResponse::getTimestamp));

        List<LogEntryResponse> results = new ArrayList<>();
        for (LogEntryResponse entry : arr) {
            String ts = entry.getTimestamp();
            if (ts.compareTo(start) >= 0 && ts.compareTo(end) <= 0) {
                results.add(entry);
            }
        }
        return results;
    }

    /**
     * Detects anomaly bursts using sliding window algorithm.
     * Returns merged summary strings for each anomaly period.
     *
     * Time Complexity: O(n log n) sort + O(n) sliding window
     * Space Complexity: O(n + b) — b = burst count
     */
    @Override
    public List<String> getAnomalySummary(
            int windowSize, int errorThreshold) {

        LogEntryResponse[] arr = getCacheAsArray();
        if (arr.length < windowSize) return new ArrayList<>();

        // Sort by timestamp for meaningful time-based windows
        Arrays.sort(arr, Comparator.comparing(
                LogEntryResponse::getTimestamp));

        // Build first window
        int errorCount = 0;
        for (int i = 0; i < windowSize; i++) {
            if (arr[i].getSeverityScore() == 3) errorCount++;
        }

        List<Integer> burstStarts = new ArrayList<>();
        if (errorCount >= errorThreshold) burstStarts.add(0);

        // Slide window forward
        for (int i = windowSize; i < arr.length; i++) {
            if (arr[i].getSeverityScore() == 3)              errorCount++;
            if (arr[i - windowSize].getSeverityScore() == 3) errorCount--;
            if (errorCount >= errorThreshold) {
                burstStarts.add(i - windowSize + 1);
            }
        }

        // Merge overlapping bursts into summary strings
        return mergeBursts(arr, burstStarts, windowSize);
    }

    /**
     * Deletes a log entry by timestamp.
     * Throws LogNotFoundException if timestamp does not exist.
     *
     * Time Complexity: O(1) — ConcurrentHashMap.remove()
     */
    @Override
    public boolean deleteLog(String timestamp) {
        if (!logCache.containsKey(timestamp)) {
            throw new LogNotFoundException(timestamp);
        }
        logCache.remove(timestamp);
        return true;
    }

    /**
     * Clears all log entries from the in-memory cache.
     * Time Complexity: O(n)
     */
    @Override
    public void clearAllLogs() {
        logCache.clear();
    }

    /**
     * Export placeholder — file persistence added in next iteration.
     * In v2 the primary storage is the REST API consumer's responsibility.
     */
    @Override
    public void exportLogs(String filePath) {
        // Phase 2 — file export via FileHandler
        // For now logs live in-memory for the REST API lifecycle
    }

    // ── Algorithm implementations ─────────────────────────────────────────

    /**
     * Cycle Sort — sorts LogEntryResponse array by severityScore ascending.
     * Minimizes memory writes — each element moves at most once.
     *
     * Time Complexity: O(n²)
     * Space Complexity: O(1) — in-place
     */
    private void cycleSort(LogEntryResponse[] arr) {
        int n = arr.length;
        for (int cycleStart = 0; cycleStart < n - 1; cycleStart++) {
            LogEntryResponse item = arr[cycleStart];
            int pos = cycleStart;

            for (int i = cycleStart + 1; i < n; i++) {
                if (arr[i].getSeverityScore() < item.getSeverityScore()) {
                    pos++;
                }
            }
            if (pos == cycleStart) continue;

            while (item.getSeverityScore()
                    == arr[pos].getSeverityScore()) pos++;

            LogEntryResponse temp = arr[pos];
            arr[pos] = item;
            item = temp;

            while (pos != cycleStart) {
                pos = cycleStart;
                for (int i = cycleStart + 1; i < n; i++) {
                    if (arr[i].getSeverityScore()
                            < item.getSeverityScore()) pos++;
                }
                while (item.getSeverityScore()
                        == arr[pos].getSeverityScore()) pos++;
                temp = arr[pos];
                arr[pos] = item;
                item = temp;
            }
        }
    }

    /**
     * Binary Search + expand — finds ALL entries matching targetScore.
     *
     * Time Complexity: O(log n + d) — d = matching entries
     * Space Complexity: O(d)
     */
    private List<LogEntryResponse> searchAllBySeverity(
            LogEntryResponse[] sortedArr, int targetScore) {

        List<LogEntryResponse> results = new ArrayList<>();
        int low = 0, high = sortedArr.length - 1, index = -1;

        while (low <= high) {
            int mid = low + (high - low) / 2;
            int midScore = sortedArr[mid].getSeverityScore();
            if (midScore == targetScore) { index = mid; break; }
            else if (midScore < targetScore) low = mid + 1;
            else high = mid - 1;
        }

        if (index == -1) return results;

        int left = index - 1;
        while (left >= 0
                && sortedArr[left].getSeverityScore() == targetScore)
            left--;

        int right = index + 1;
        while (right < sortedArr.length
                && sortedArr[right].getSeverityScore() == targetScore)
            right++;

        for (int i = left + 1; i < right; i++) {
            results.add(sortedArr[i]);
        }
        return results;
    }

    /**
     * Merges overlapping burst windows into distinct anomaly periods.
     * Returns human-readable summary strings.
     */
    private List<String> mergeBursts(LogEntryResponse[] arr,
                                     List<Integer> starts,
                                     int windowSize) {
        List<String> summaries = new ArrayList<>();
        if (starts.isEmpty()) return summaries;

        int mergeStart = starts.get(0);
        int mergeEnd   = mergeStart + windowSize - 1;
        int peakErrors = 0;

        for (int i = 0; i < starts.size(); i++) {
            int cs = starts.get(i);
            int ce = cs + windowSize - 1;
            int ec = 0;
            for (int j = cs; j <= Math.min(ce, arr.length - 1); j++) {
                if (arr[j].getSeverityScore() == 3) ec++;
            }
            peakErrors = Math.max(peakErrors, ec);

            boolean isLast     = (i == starts.size() - 1);
            boolean nextOverlap= !isLast && starts.get(i+1) <= ce;

            if (!nextOverlap || isLast) {
                mergeEnd = Math.min(ce, arr.length - 1);
                summaries.add(String.format(
                        "Anomaly period: %s → %s | Peak ERRORs: %d in window of %d",
                        arr[mergeStart].getTimestamp(),
                        arr[mergeEnd].getTimestamp(),
                        peakErrors, windowSize));
                if (!isLast) {
                    mergeStart = starts.get(i + 1);
                    mergeEnd   = mergeStart + windowSize - 1;
                    peakErrors = 0;
                }
            }
        }
        return summaries;
    }
}