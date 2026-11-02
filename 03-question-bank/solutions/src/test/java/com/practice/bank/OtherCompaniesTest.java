package com.practice.bank;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtherCompaniesTest {
    static TreeCodec.Node n(int v, TreeCodec.Node l, TreeCodec.Node r) { return new TreeCodec.Node(v, l, r); }
    static TreeCodec.Node n(int v) { return new TreeCodec.Node(v); }

    //      1
    //     / \
    //    2   3
    //       / \
    //      4   5
    final TreeCodec.Node tree = n(1, n(2), n(3, n(4), n(5)));

    // ---- Amazon: tree serialize / deserialize ----
    @Test
    void preorderRoundTrip() {
        String s = TreeCodec.serialize(tree);
        assertEquals("1,2,#,#,3,4,#,#,5,#,#", s);
        assertTrue(TreeCodec.Node.same(tree, TreeCodec.deserialize(s)));
        assertEquals("#", TreeCodec.serialize(null));
        assertEquals(null, TreeCodec.deserialize("#"));
        assertThrows(IllegalArgumentException.class, () -> TreeCodec.deserialize("1,#"));
    }

    @Test
    void bracketRoundTrip() {
        for (String s : List.of("4(2(3)(1))(6(5))", "1()(4)", "-12(5)", "7", "")) {
            assertEquals(s, TreeCodec.toBrackets(TreeCodec.fromBrackets(s)));
        }
        assertEquals("1(2)(3(4)(5))", TreeCodec.toBrackets(tree));
        assertThrows(IllegalArgumentException.class, () -> TreeCodec.fromBrackets("1(2"));
        assertThrows(IllegalArgumentException.class, () -> TreeCodec.fromBrackets("1(2))"));
    }

    // ---- Amazon: word break ----
    final Set<String> dict = Set.of("apple", "pen", "applepen", "pine", "pineapple", "cats", "dog", "sand", "and", "cat");

    @Test
    void wordBreak() {
        assertTrue(WordBreak.canSplit("applepenapple", dict));
        assertTrue(!WordBreak.canSplit("catsandog", dict));
        assertEquals(Set.of("cat sand dog", "cats and dog"), Set.copyOf(WordBreak.allSplits("catsanddog", dict)));
        assertEquals(Set.of("pine apple pen apple", "pineapple pen apple", "pine applepen apple"),
                Set.copyOf(WordBreak.allSplits("pineapplepenapple", dict)));
        assertTrue(WordBreak.allSplits("catsandog", dict).isEmpty());
    }

    @Test
    void findAllWordsWithTrie() {
        assertEquals(List.of(
                new WordBreak.Found(0, "cat"), new WordBreak.Found(0, "cats"),
                new WordBreak.Found(3, "sand"), new WordBreak.Found(4, "and"), new WordBreak.Found(7, "dog")),
                WordBreak.findAll("catsanddog", dict));
    }

    // ---- Visa: merge intervals ----
    @Test
    void mergeIntervals() {
        List<int[]> out = MergeIntervals.merge(List.of(new int[]{8, 10}, new int[]{1, 3}, new int[]{2, 6}, new int[]{15, 18}));
        assertEquals(List.of("1-6", "8-10", "15-18"), out.stream().map(a -> a[0] + "-" + a[1]).toList());
        assertEquals(List.of("1-5"), MergeIntervals.merge(List.of(new int[]{1, 4}, new int[]{4, 5}))
                .stream().map(a -> a[0] + "-" + a[1]).toList());
        assertTrue(MergeIntervals.merge(List.of()).isEmpty());
    }
}
