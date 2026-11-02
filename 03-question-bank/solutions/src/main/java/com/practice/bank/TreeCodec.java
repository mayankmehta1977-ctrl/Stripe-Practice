package com.practice.bank;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Asked at Amazon: print a binary tree depth-first to a string, then parse it back.
 * Format A (LeetCode 297): pre-order with '#' for missing children: "1,2,#,#,3,#,#".
 * Format B (LeetCode 606 / 536): brackets: "4(2(3)(1))(6(5))", with "()" for a
 *          missing left child when a right child exists: "1()(4)".
 */
public final class TreeCodec {
    private TreeCodec() {}

    public static final class Node {
        public final int val;
        public Node left, right;

        public Node(int val) { this.val = val; }

        public Node(int val, Node left, Node right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }

        public static boolean same(Node a, Node b) {
            if (a == null || b == null) return a == b;
            return a.val == b.val && same(a.left, b.left) && same(a.right, b.right);
        }
    }

    // ---- Format A: pre-order with null markers ----------------------------

    public static String serialize(Node root) {
        List<String> out = new ArrayList<>();
        Deque<Node> stack = new ArrayDeque<>();           // iterative pre-order
        stack.push(root == null ? NULL_NODE : root);
        while (!stack.isEmpty()) {
            Node n = stack.pop();
            if (n == NULL_NODE) {
                out.add("#");
                continue;
            }
            out.add(Integer.toString(n.val));
            stack.push(n.right == null ? NULL_NODE : n.right);   // right first, so left pops first
            stack.push(n.left == null ? NULL_NODE : n.left);
        }
        return String.join(",", out);
    }

    private static final Node NULL_NODE = new Node(0);   // sentinel: ArrayDeque can't hold null

    public static Node deserialize(String data) {
        Iterator<String> tokens = List.of(data.split(",")).iterator();
        Node root = build(tokens);
        if (tokens.hasNext()) throw new IllegalArgumentException("trailing tokens");
        return root;
    }

    private static Node build(Iterator<String> tokens) {
        if (!tokens.hasNext()) throw new IllegalArgumentException("input ended early");
        String t = tokens.next().trim();
        if (t.equals("#")) return null;
        Node n = new Node(Integer.parseInt(t));
        n.left = build(tokens);
        n.right = build(tokens);
        return n;
    }

    // ---- Format B: brackets -----------------------------------------------

    public static String toBrackets(Node n) {
        if (n == null) return "";
        StringBuilder sb = new StringBuilder(Integer.toString(n.val));
        if (n.left != null || n.right != null) sb.append('(').append(toBrackets(n.left)).append(')');
        if (n.right != null) sb.append('(').append(toBrackets(n.right)).append(')');
        return sb.toString();
    }

    public static Node fromBrackets(String s) {
        if (s.isEmpty()) return null;
        int[] i = {0};                                     // shared index into s
        Node root = parseNode(s, i);
        if (i[0] != s.length()) throw new IllegalArgumentException("unexpected text at " + i[0]);
        return root;
    }

    private static Node parseNode(String s, int[] i) {
        if (i[0] >= s.length() || s.charAt(i[0]) == ')') return null;   // empty child "()"
        int start = i[0];
        if (s.charAt(i[0]) == '-') i[0]++;
        while (i[0] < s.length() && Character.isDigit(s.charAt(i[0]))) i[0]++;
        if (start == i[0] || (i[0] - start == 1 && s.charAt(start) == '-')) {
            throw new IllegalArgumentException("expected a number at " + start);
        }
        Node n = new Node(Integer.parseInt(s.substring(start, i[0])));
        if (i[0] < s.length() && s.charAt(i[0]) == '(') {
            i[0]++;
            n.left = parseNode(s, i);
            expect(s, i, ')');
        }
        if (i[0] < s.length() && s.charAt(i[0]) == '(') {
            i[0]++;
            n.right = parseNode(s, i);
            expect(s, i, ')');
        }
        return n;
    }

    private static void expect(String s, int[] i, char c) {
        if (i[0] >= s.length() || s.charAt(i[0]) != c) {
            throw new IllegalArgumentException("expected '" + c + "' at " + i[0]);
        }
        i[0]++;
    }
}
