package com.practice.bank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Asked at Amazon: find words in a string of letters.
 * Part 1 (LeetCode 139): can the string be split entirely into dictionary words?
 * Part 2 (LeetCode 140): list every way to split it.
 * Part 3: list every dictionary word appearing anywhere, using a trie.
 */
public final class WordBreak {
    private WordBreak() {}

    public static boolean canSplit(String s, Set<String> words) {
        int maxLen = words.stream().mapToInt(String::length).max().orElse(0);
        boolean[] dp = new boolean[s.length() + 1];      // dp[i]: s[0..i) can be split
        dp[0] = true;
        for (int i = 1; i <= s.length(); i++) {
            for (int j = Math.max(0, i - maxLen); j < i && !dp[i]; j++) {
                dp[i] = dp[j] && words.contains(s.substring(j, i));
            }
        }
        return dp[s.length()];
    }

    public static List<String> allSplits(String s, Set<String> words) {
        return splitsFrom(0, s, words, new HashMap<>());
    }

    private static List<String> splitsFrom(int start, String s, Set<String> words, Map<Integer, List<String>> memo) {
        if (start == s.length()) return List.of("");
        if (memo.containsKey(start)) return memo.get(start);
        List<String> out = new ArrayList<>();
        for (int end = start + 1; end <= s.length(); end++) {
            String w = s.substring(start, end);
            if (!words.contains(w)) continue;
            for (String rest : splitsFrom(end, s, words, memo)) {
                out.add(rest.isEmpty() ? w : w + " " + rest);
            }
        }
        memo.put(start, out);
        return out;
    }

    public record Found(int index, String word) {}

    public static List<Found> findAll(String s, Set<String> words) {
        TrieNode root = new TrieNode();
        for (String w : words) {
            TrieNode n = root;
            for (char c : w.toCharArray()) n = n.next.computeIfAbsent(c, k -> new TrieNode());
            n.word = w;
        }
        List<Found> found = new ArrayList<>();
        for (int start = 0; start < s.length(); start++) {
            TrieNode n = root;
            for (int k = start; k < s.length(); k++) {
                n = n.next.get(s.charAt(k));
                if (n == null) break;                    // no word continues this way
                if (n.word != null) found.add(new Found(start, n.word));
            }
        }
        return found;
    }

    private static final class TrieNode {
        final Map<Character, TrieNode> next = new HashMap<>();
        String word;
    }
}
