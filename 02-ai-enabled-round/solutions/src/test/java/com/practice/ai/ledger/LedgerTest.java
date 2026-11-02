package com.practice.ai.ledger;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.practice.ai.ledger.Ledger.Reason.CONFLICTING_DUPLICATE;
import static com.practice.ai.ledger.Ledger.Reason.INSUFFICIENT_FUNDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LedgerTest {

    // ---- Part 1 ----
    @Test
    void finalBalancesSortedByAccount() {
        Ledger l = Ledger.allowingOverdrafts(List.of(
                "t1,1,bob,500", "t2,2,alice,1000", "t3,3,bob,-200", "t4,4,alice,-1500"));
        assertEquals(Map.of("alice", -500L, "bob", 300L), l.balances());
        assertEquals(List.of("alice", "bob"), List.copyOf(l.balances().keySet()));
    }

    @Test
    void blankLinesIgnoredAndMalformedLinesReported() {
        assertEquals(100L, Ledger.of(List.of("", "t1,1,a,100", "  ")).balance("a"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Ledger.of(List.of("t1,1,a,100", "t2,x,a,5")));
        assertTrue(e.getMessage().contains("line 2"));
        assertThrows(IllegalArgumentException.class, () -> Ledger.of(List.of("t1,1,a")));
    }

    // ---- Part 2 ----
    @Test
    void processesInTimestampOrderNotInputOrder() {
        // The debit at ts=5 is listed first but happens after the deposit at ts=1.
        Ledger l = Ledger.of(List.of("t2,5,a,-300", "t1,1,a,500"));
        assertEquals(200L, l.balance("a"));
        assertTrue(l.rejected().isEmpty());
    }

    @Test
    void rejectsOverdraftAndKeepsGoing() {
        Ledger l = Ledger.of(List.of("t1,1,a,100", "t2,2,a,-150", "t3,3,a,-100"));
        assertEquals(0L, l.balance("a"));
        assertEquals(Map.of("t2", INSUFFICIENT_FUNDS), l.rejected());
    }

    @Test
    void sameTimestampUsesInputOrder() {
        Ledger l = Ledger.of(List.of("t1,1,a,-50", "t2,1,a,100"));
        assertEquals(Map.of("t1", INSUFFICIENT_FUNDS), l.rejected());
        assertEquals(100L, l.balance("a"));
    }

    // ---- Part 3 ----
    @Test
    void transferMovesMoneyAtomically() {
        Ledger l = Ledger.of(List.of("t1,1,a,500", "t2,2,TRANSFER,a,b,200", "t3,3,TRANSFER,a,b,400"));
        assertEquals(300L, l.balance("a"));
        assertEquals(200L, l.balance("b"));
        assertEquals(Map.of("t3", INSUFFICIENT_FUNDS), l.rejected());   // and b got nothing from t3
    }

    @Test
    void invalidTransfersAreRejectedAtParse() {
        assertThrows(IllegalArgumentException.class, () -> Ledger.of(List.of("t1,1,TRANSFER,a,b,-5")));
        assertThrows(IllegalArgumentException.class, () -> Ledger.of(List.of("t1,1,TRANSFER,a,a,5")));
    }

    // ---- Part 4 ----
    @Test
    void exactRetryAppliedOnce() {
        Ledger l = Ledger.of(List.of("t1,1,a,100", "t1,1,a,100", "t1,1,a,100"));
        assertEquals(100L, l.balance("a"));
        assertTrue(l.rejected().isEmpty());
    }

    @Test
    void conflictingDuplicateIsRejected() {
        Ledger l = Ledger.of(List.of("t1,1,a,100", "t1,1,a,999"));
        assertEquals(100L, l.balance("a"));
        assertEquals(Map.of("t1#2", CONFLICTING_DUPLICATE), l.rejected());
    }

    @Test
    void balanceAtPointInTime() {
        Ledger l = Ledger.of(List.of("t1,10,a,100", "t2,20,a,-30", "t3,30,TRANSFER,a,b,50"));
        assertEquals(0L, l.balanceAt("a", 5));
        assertEquals(100L, l.balanceAt("a", 10));
        assertEquals(70L, l.balanceAt("a", 25));
        assertEquals(20L, l.balanceAt("a", 30));
        assertEquals(50L, l.balanceAt("b", 99));
        assertEquals(0L, l.balanceAt("nobody", 99));
    }
}
