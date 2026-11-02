"""
Solutions to the two reported interview questions, with tests.

Q1  Print a tree depth-first into a string, then parse that string back
    into the same tree.            (LeetCode 297, plus an N-ary variant)
Q2  Find the words hidden in a string of letters.
    (LeetCode 139 / 140 Word Break, plus the "all substrings" variant)

Run:  python3 interview_solutions.py
"""

from __future__ import annotations
from dataclasses import dataclass, field
from functools import lru_cache
from typing import Optional


# ============================================================================
# Q1a  Binary tree: depth-first (pre-order) serialize / deserialize
# ============================================================================

@dataclass
class Node:
    val: str
    left: Optional["Node"] = None
    right: Optional["Node"] = None


NULL = "#"
SEP = ","


def serialize(root: Optional[Node]) -> str:
    """Pre-order DFS: node, left subtree, right subtree.
    Missing children are written as '#', so the shape is unambiguous.
    O(n) time, O(h) recursion stack."""
    out: list[str] = []

    def dfs(node: Optional[Node]) -> None:
        if node is None:
            out.append(NULL)
            return
        out.append(node.val)
        dfs(node.left)
        dfs(node.right)

    dfs(root)
    return SEP.join(out)


def deserialize(data: str) -> Optional[Node]:
    """Read tokens in the same pre-order they were written.
    An iterator means each token is consumed exactly once: O(n)."""
    tokens = iter(data.split(SEP))

    def build() -> Optional[Node]:
        tok = next(tokens)
        if tok == NULL:
            return None
        node = Node(tok)
        node.left = build()
        node.right = build()
        return node

    return build()


# Follow-up interviewers like: "Can you do it without recursion?"
def serialize_iterative(root: Optional[Node]) -> str:
    out, stack = [], [root]
    while stack:
        node = stack.pop()
        if node is None:
            out.append(NULL)
            continue
        out.append(node.val)
        stack.append(node.right)  # push right first so left is processed first
        stack.append(node.left)
    return SEP.join(out)


# ============================================================================
# Q1b  N-ary tree with a bracket format:  A(B(D,E),C)
#      A common "print it, then parse it" variant that tests parsing skill.
# ============================================================================

@dataclass
class NNode:
    val: str
    children: list["NNode"] = field(default_factory=list)


def to_brackets(node: NNode) -> str:
    """Depth-first print: value, then '(' children ')' if any."""
    if not node.children:
        return node.val
    return node.val + "(" + ",".join(to_brackets(c) for c in node.children) + ")"


def from_brackets(s: str) -> NNode:
    """Recursive-descent parser with a single index pointer.
    Grammar:  node := NAME [ '(' node { ',' node } ')' ]
    O(n) time. Raises ValueError on malformed input."""
    i = 0

    def parse_node() -> NNode:
        nonlocal i
        start = i
        while i < len(s) and s[i] not in "(),":
            i += 1
        if i == start:
            raise ValueError(f"expected a name at position {i}")
        node = NNode(s[start:i])
        if i < len(s) and s[i] == "(":
            i += 1  # skip '('
            node.children.append(parse_node())
            while i < len(s) and s[i] == ",":
                i += 1
                node.children.append(parse_node())
            if i >= len(s) or s[i] != ")":
                raise ValueError(f"expected ')' at position {i}")
            i += 1  # skip ')'
        return node

    root = parse_node()
    if i != len(s):
        raise ValueError(f"unexpected trailing text at position {i}")
    return root


# ============================================================================
# Q2  Find words in a string of letters
# ============================================================================

def word_break(s: str, words: set[str]) -> bool:
    """LC 139: can s be split entirely into dictionary words?
    dp[i] = True if s[:i] can be split.  O(n * L) with L = longest word."""
    max_len = max(map(len, words), default=0)
    dp = [False] * (len(s) + 1)
    dp[0] = True
    for i in range(1, len(s) + 1):
        for j in range(max(0, i - max_len), i):
            if dp[j] and s[j:i] in words:
                dp[i] = True
                break
    return dp[-1]


def word_break_all(s: str, words: set[str]) -> list[str]:
    """LC 140: return every way to split s into dictionary words.
    Memoized DFS on the start index; output can be exponential in the worst case."""
    max_len = max(map(len, words), default=0)

    @lru_cache(maxsize=None)
    def from_index(i: int) -> list[list[str]]:
        if i == len(s):
            return [[]]
        results = []
        for j in range(i + 1, min(len(s), i + max_len) + 1):
            w = s[i:j]
            if w in words:
                for rest in from_index(j):
                    results.append([w] + rest)
        return results

    return [" ".join(parts) for parts in from_index(0)]


class Trie:
    def __init__(self, words: set[str]):
        self.root: dict = {}
        for w in words:
            node = self.root
            for ch in w:
                node = node.setdefault(ch, {})
            node["$"] = w  # end-of-word marker holds the word


def find_all_words(s: str, words: set[str]) -> list[tuple[int, str]]:
    """Variant: list every dictionary word appearing anywhere in s
    (overlaps allowed), with its start index.
    A trie lets each start position stop as soon as no word can continue:
    O(n * L) instead of checking every substring against the set."""
    trie = Trie(words)
    found = []
    for start in range(len(s)):
        node = trie.root
        for k in range(start, len(s)):
            node = node.get(s[k])
            if node is None:
                break
            if "$" in node:
                found.append((start, node["$"]))
    return found


# ============================================================================
# Tests
# ============================================================================

def _same(a: Optional[Node], b: Optional[Node]) -> bool:
    if a is None or b is None:
        return a is b
    return a.val == b.val and _same(a.left, b.left) and _same(a.right, b.right)


def run_tests() -> None:
    #      1
    #     / \
    #    2   3
    #       / \
    #      4   5
    t = Node("1", Node("2"), Node("3", Node("4"), Node("5")))
    s = serialize(t)
    assert s == "1,2,#,#,3,4,#,#,5,#,#", s
    assert serialize_iterative(t) == s
    assert _same(deserialize(s), t)
    assert serialize(None) == "#" and deserialize("#") is None
    # skewed tree (recursion depth = n)
    skew = Node("a", Node("b", Node("c")))
    assert _same(deserialize(serialize(skew)), skew)

    n = NNode("A", [NNode("B", [NNode("D"), NNode("E")]), NNode("C")])
    b = to_brackets(n)
    assert b == "A(B(D,E),C)", b
    assert to_brackets(from_brackets(b)) == b
    assert to_brackets(from_brackets("root")) == "root"
    for bad in ["A(B", "A(B))", "(B)", "A(,B)"]:
        try:
            from_brackets(bad)
            raise AssertionError(f"should reject {bad!r}")
        except ValueError:
            pass

    d = {"apple", "pen", "applepen", "pine", "pineapple", "cats", "dog", "sand", "and", "cat"}
    assert word_break("applepenapple", d)
    assert not word_break("catsandog", d)
    assert sorted(word_break_all("pineapplepenapple", d)) == sorted([
        "pine apple pen apple", "pineapple pen apple", "pine applepen apple"])
    assert sorted(word_break_all("catsanddog", d)) == ["cat sand dog", "cats and dog"]
    assert word_break_all("catsandog", d) == []

    assert find_all_words("catsanddog", d) == [
        (0, "cat"), (0, "cats"), (3, "sand"), (4, "and"), (7, "dog")]

    print("All tests passed.")


if __name__ == "__main__":
    run_tests()
