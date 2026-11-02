package com.practice.ai.ledger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reference solution for the progressive transaction-ledger problem.
 * Amounts are integer cents. Input is one transaction per line.
 *
 * Part 1: "id,timestamp,account,amount"  -> final balance per account.
 * Part 2: process in timestamp order; reject any transaction that would make
 *         a balance negative.
 * Part 3: transfers "id,timestamp,TRANSFER,from,to,amount" - all or nothing.
 * Part 4: retried transactions repeat an id. Apply each id once; a repeat with
 *         different details is a conflict. Also: balance of an account at a time.
 */
public class Ledger {

    public enum Reason { INSUFFICIENT_FUNDS, CONFLICTING_DUPLICATE }

    /** One change to one account's balance. */
    record Posting(String account, long delta) {}

    record Txn(String id, long timestamp, List<Posting> postings, int inputOrder) {}

    private final boolean allowOverdraft;
    private final Map<String, Long> balances = new TreeMap<>();
    private final Map<String, Reason> rejected = new LinkedHashMap<>();
    private final Map<String, Txn> applied = new HashMap<>();
    private final Map<String, TreeMap<Long, Long>> history = new HashMap<>();

    /** Part 1 behavior: overdrafts allowed. */
    public static Ledger allowingOverdrafts(List<String> lines) {
        return new Ledger(lines, true);
    }

    /** Part 2+ behavior: overdrafts rejected. */
    public static Ledger of(List<String> lines) {
        return new Ledger(lines, false);
    }

    private Ledger(List<String> lines, boolean allowOverdraft) {
        this.allowOverdraft = allowOverdraft;
        List<Txn> txns = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.isEmpty()) txns.add(parse(line, i));
        }
        // Stable order: timestamp, then position in the input.
        txns.sort(Comparator.comparingLong(Txn::timestamp).thenComparingInt(Txn::inputOrder));
        txns.forEach(this::process);
    }

    // ---- parsing ---------------------------------------------------------

    static Txn parse(String line, int lineIndex) {
        String[] f = line.split(",", -1);
        try {
            if (f.length == 4) {
                return new Txn(f[0].trim(), Long.parseLong(f[1].trim()),
                        List.of(new Posting(f[2].trim(), Long.parseLong(f[3].trim()))), lineIndex);
            }
            if (f.length == 6 && f[2].trim().equals("TRANSFER")) {
                long amount = Long.parseLong(f[5].trim());
                if (amount <= 0) throw new IllegalArgumentException("transfer amount must be positive");
                String from = f[3].trim(), to = f[4].trim();
                if (from.equals(to)) throw new IllegalArgumentException("transfer to the same account");
                return new Txn(f[0].trim(), Long.parseLong(f[1].trim()),
                        List.of(new Posting(from, -amount), new Posting(to, amount)), lineIndex);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("line " + (lineIndex + 1) + ": bad number in '" + line + "'");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("line " + (lineIndex + 1) + ": " + e.getMessage());
        }
        throw new IllegalArgumentException("line " + (lineIndex + 1) + ": unrecognized format '" + line + "'");
    }

    // ---- processing ------------------------------------------------------

    private void process(Txn t) {
        Txn previous = applied.get(t.id());
        if (previous != null) {                                      // Part 4
            if (!previous.postings().equals(t.postings()) || previous.timestamp() != t.timestamp()) {
                rejected.put(t.id() + "#" + (t.inputOrder() + 1), Reason.CONFLICTING_DUPLICATE);
            }
            return;                                                  // exact retry: ignore
        }
        if (!allowOverdraft) {                                       // Part 2/3: check all postings first
            for (Posting p : t.postings()) {
                if (balance(p.account()) + p.delta() < 0) {
                    rejected.put(t.id(), Reason.INSUFFICIENT_FUNDS);
                    return;
                }
            }
        }
        for (Posting p : t.postings()) {
            long updated = balance(p.account()) + p.delta();
            balances.put(p.account(), updated);
            history.computeIfAbsent(p.account(), k -> new TreeMap<>()).put(t.timestamp(), updated);
        }
        applied.put(t.id(), t);
    }

    // ---- queries ---------------------------------------------------------

    public long balance(String account) {
        return balances.getOrDefault(account, 0L);
    }

    /** All balances, sorted by account id. */
    public Map<String, Long> balances() {
        return new TreeMap<>(balances);
    }

    /**
     * Rejected transactions in processing order. Conflicting duplicates are
     * keyed "id#line" so they don't collide with the original id.
     */
    public Map<String, Reason> rejected() {
        return new LinkedHashMap<>(rejected);
    }

    /** Part 4: balance after every transaction with timestamp <= ts. */
    public long balanceAt(String account, long ts) {
        TreeMap<Long, Long> h = history.get(account);
        if (h == null) return 0L;
        Map.Entry<Long, Long> e = h.floorEntry(ts);
        return e == null ? 0L : e.getValue();
    }
}
