package com.logshield.logshieldv2.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for LogEntryEntity.
 *
 * Spring generates all SQL automatically from method names and
 * @Query annotations — no boilerplate DAO code needed.
 *
 * JpaRepository<LogEntryEntity, UUID> provides:
 * - save(), findById(), findAll(), deleteById(), count()
 * - All wrapped in transactions automatically
 *
 * Custom queries use JPQL (Java Persistence Query Language) —
 * object-oriented SQL that references entity fields not column names.
 */
@Repository
public interface LogEntryRepository
        extends JpaRepository<LogEntryEntity, UUID> {

    // Find all entries with a specific severity score
    // Spring generates: SELECT * FROM log_entries WHERE severity_score = ?
    // Time Complexity: O(log n) with DB index on severity_score
    List<LogEntryEntity> findBySeverityScore(int severityScore);

    // Find entries sorted by severity ascending
    // Used by getSortedLogs() — DB handles sort, no Cycle Sort needed
    List<LogEntryEntity> findAllByOrderBySeverityScoreAsc();

    // Find entries within a timestamp range
    // Replaces the manual time-range filter — DB WHERE clause is O(log n)
    @Query("SELECT e FROM LogEntryEntity e " +
            "WHERE e.timestamp >= :start AND e.timestamp <= :end " +
            "ORDER BY e.timestamp ASC")
    List<LogEntryEntity> findByTimestampRange(
            @Param("start") String start,
            @Param("end")   String end);

    // Count entries by level — used by statistics dashboard
    long countByLevel(String level);
}