package com.logshield.logshieldv2.trie;

import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Spring-managed service wrapping the Trie data structure.
 *
 * Annotated with @Component — Spring creates one instance and
 * manages its lifecycle. TrieService will be injected into
 * LogShieldServiceImpl via constructor injection.
 *
 * Single Responsibility — owns all pattern tracking logic.
 * LogShieldServiceImpl delegates here instead of managing
 * Trie internals directly.
 *
 * Time Complexity: all operations O(L) or O(L+W) — see Trie.java
 * Space Complexity: O(total characters across all inserted patterns)
 */
@Component
public class TrieService {

    private final Trie trie;

    public TrieService() {
        this.trie = new Trie();
    }

    /**
     * Tracks a log message pattern — inserts into Trie and
     * increments frequency if already present.
     * Time Complexity: O(L)
     */
    public void trackPattern(String message) {
        if (message == null || message.isEmpty()) return;
        trie.insert(message);
    }

    /**
     * Returns frequency of exact pattern match.
     * Time Complexity: O(L)
     */
    public int getFrequency(String pattern) {
        if (pattern == null || pattern.isEmpty()) return 0;
        return trie.getFrequency(pattern);
    }

    /**
     * Returns all patterns starting with the given prefix.
     * Time Complexity: O(L + W)
     */
    public List<String> searchByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return List.of();
        return trie.searchByPrefix(prefix);
    }

    /**
     * Resets the Trie — called before reloading from file
     * to prevent frequency double-counting.
     * Time Complexity: O(1)
     */
    public void reset() {
        trie.reset();
    }
}