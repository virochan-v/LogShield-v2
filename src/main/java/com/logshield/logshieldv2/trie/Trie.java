package com.logshield.logshieldv2.trie;

import java.util.ArrayList;
import java.util.List;

/**
 * Trie data structure for log message pattern indexing.
 *
 * Stores log messages character by character — nodes sharing
 * a common prefix share the same path in the tree.
 *
 * Core advantage over HashMap:
 * - HashMap: exact key match only — O(1)
 * - Trie: prefix search in O(L+W) — impossible with HashMap
 *
 * Where L = prefix length, W = total chars in matching words.
 */
public class Trie {

    // Root node — empty, never holds a character
    private final TrieNode root;

    public Trie() {
        this.root = new TrieNode();
    }

    // -------------------------------------------------------------------------
    // insert(String word)
    //
    // Time Complexity : O(L) — traverse L nodes, create if missing
    // Space Complexity: O(L) worst case — L new nodes for unique pattern
    //
    // Called every time a log entry is added or loaded from file.
    // Shared prefixes reuse existing nodes — no duplication.
    // -------------------------------------------------------------------------

    /**
     * Inserts a log message pattern into the Trie.
     * If the pattern already exists, increments its frequency.
     *
     * @param word the log message to insert
     */
    public void insert(String word) {
        if (word == null || word.isEmpty()) return;

        TrieNode current = root;

        // Walk one character at a time — create nodes that don't exist
        for (char ch : word.toCharArray()) {
            current.children.putIfAbsent(ch, new TrieNode());
            current = current.children.get(ch);
        }

        // Mark end of pattern and increment frequency
        current.isEndOfWord = true;
        current.frequency++;
    }

    // -------------------------------------------------------------------------
    // search(String word) → boolean
    //
    // Time Complexity : O(L) — traverse L nodes
    // Space Complexity: O(1) — no extra memory
    //
    // Returns true only if the exact pattern was inserted before.
    // -------------------------------------------------------------------------

    /**
     * Returns true if the exact pattern exists in the Trie.
     *
     * @param word the pattern to search for
     * @return true if found, false otherwise
     */
    public boolean search(String word) {
        if (word == null || word.isEmpty()) return false;

        TrieNode current = root;

        for (char ch : word.toCharArray()) {
            if (!current.children.containsKey(ch)) return false;
            current = current.children.get(ch);
        }

        // Must be a terminal node — not just a prefix of another word
        return current.isEndOfWord;
    }

    // -------------------------------------------------------------------------
    // getFrequency(String word) → int
    //
    // Time Complexity : O(L) — traverse to terminal node
    // Space Complexity: O(1)
    //
    // Returns 0 if pattern was never inserted.
    // -------------------------------------------------------------------------

    /**
     * Returns how many times the exact pattern was inserted.
     *
     * @param word the pattern to check
     * @return frequency count, 0 if not found
     */
    public int getFrequency(String word) {
        if (word == null || word.isEmpty()) return 0;

        TrieNode current = root;

        for (char ch : word.toCharArray()) {
            if (!current.children.containsKey(ch)) return 0;
            current = current.children.get(ch);
        }

        return current.isEndOfWord ? current.frequency : 0;
    }

    // -------------------------------------------------------------------------
    // searchByPrefix(String prefix) → List<String>
    //
    // Time Complexity : O(L + W)
    //   L = prefix length — traverse to prefix endpoint
    //   W = total characters across all matching words — DFS collection
    //
    // Space Complexity: O(W) — result list
    //
    // This is the core Trie advantage — HashMap cannot do this.
    // "NullPointer" finds every NullPointerException variant in O(L+W).
    // -------------------------------------------------------------------------

    /**
     * Returns all patterns that start with the given prefix.
     *
     * Traverses to the prefix endpoint in O(L), then performs
     * a depth-first search to collect all complete words below — O(W).
     *
     * @param prefix the search prefix — can be partial word
     * @return list of all matching complete patterns, empty if none
     */
    public List<String> searchByPrefix(String prefix) {
        List<String> results = new ArrayList<>();
        if (prefix == null || prefix.isEmpty()) return results;

        TrieNode current = root;

        // Navigate to the prefix endpoint — O(L)
        for (char ch : prefix.toCharArray()) {
            if (!current.children.containsKey(ch)) {
                // Prefix not in Trie — no matches possible
                return results;
            }
            current = current.children.get(ch);
        }

        // DFS from prefix endpoint — collect all words below — O(W)
        collectAllWords(current, prefix, results);
        return results;
    }

    // -------------------------------------------------------------------------
    // reset()
    //
    // Time Complexity : O(1) — creates new root, old tree garbage collected
    // Called when loadFromFile() reloads — prevents frequency double-counting
    // -------------------------------------------------------------------------

    /**
     * Clears the entire Trie by replacing the root.
     * Old tree is garbage collected by JVM.
     */
    public void reset() {
        root.children.clear();
        root.isEndOfWord = false;
        root.frequency   = 0;
    }

    // ── Private helper ────────────────────────────────────────────────────────

    /**
     * Recursively collects all complete words reachable from node.
     * Appends each character to the prefix built so far.
     *
     * Time Complexity: O(W) — visits every character in every matching word
     *
     * @param node    current node in DFS traversal
     * @param prefix  word built so far from root to this node
     * @param results shared list accumulating complete words
     */
    private void collectAllWords(TrieNode node,
                                 String prefix,
                                 List<String> results) {
        // If this node marks a complete word — add it
        if (node.isEndOfWord) {
            results.add(prefix);
        }

        // Recurse into every child — explore all branches
        for (var entry : node.children.entrySet()) {
            collectAllWords(
                    entry.getValue(),           // child node
                    prefix + entry.getKey(),    // extend prefix by one char
                    results
            );
        }
    }
}