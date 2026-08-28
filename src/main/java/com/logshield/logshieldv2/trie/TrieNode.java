package com.logshield.logshieldv2.trie;

import java.util.HashMap;

/**
 * Represents a single node in the Trie data structure.
 *
 * Each node stores:
 * - children: HashMap mapping character to next TrieNode
 *   HashMap chosen over array — log message patterns are sparse.
 *   26-slot array would waste memory on unused character slots.
 * - isEndOfWord: marks whether a complete pattern ends at this node
 * - frequency: how many times this pattern has been inserted
 *
 * Space Complexity per node: O(k) where k = number of actual children
 */
public class TrieNode {

    // Maps each character to its child node
    // Only branches that exist are created — memory efficient
    final HashMap<Character, TrieNode> children;

    // True if a complete log message pattern ends at this node
    boolean isEndOfWord;

    // How many times the pattern ending here has been seen
    int frequency;

    public TrieNode() {
        this.children    = new HashMap<>();
        this.isEndOfWord = false;
        this.frequency   = 0;
    }
}