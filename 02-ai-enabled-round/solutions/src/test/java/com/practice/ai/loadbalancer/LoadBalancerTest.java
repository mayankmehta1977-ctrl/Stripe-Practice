package com.practice.ai.loadbalancer;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.practice.ai.loadbalancer.LoadBalancer.REJECTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoadBalancerTest {

    static LoadBalancer withCapacities(Object... pairs) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) m.put((String) pairs[i], (Integer) pairs[i + 1]);
        return new LoadBalancer(m);
    }

    // ---- Part 1 ----
    @Test
    void roundRobinCyclesInOrder() {
        LoadBalancer lb = new LoadBalancer(List.of("a", "b", "c"));
        assertEquals(List.of("a", "b", "c", "a"),
                List.of(lb.connect("1", null), lb.connect("2", null), lb.connect("3", null), lb.connect("4", null)));
    }

    @Test
    void duplicateConnectionIdIsAnError() {
        LoadBalancer lb = new LoadBalancer(List.of("a"));
        lb.connect("1", null);
        assertThrows(IllegalArgumentException.class, () -> lb.connect("1", null));
    }

    // ---- Part 2 ----
    @Test
    void leastLoadedWithTiesBrokenByServerOrder() {
        LoadBalancer lb = withCapacities("a", 2, "b", 2);
        assertEquals("a", lb.connect("1", null));
        assertEquals("b", lb.connect("2", null));
        assertEquals("a", lb.connect("3", null));
    }

    @Test
    void rejectsWhenAllFullAndFreesCapacityOnDisconnect() {
        LoadBalancer lb = withCapacities("a", 1, "b", 1);
        lb.connect("1", null);
        lb.connect("2", null);
        assertEquals(REJECTED, lb.connect("3", null));
        lb.disconnect("1");
        lb.disconnect("1");                       // duplicate disconnect is harmless
        assertEquals("a", lb.connect("4", null));
        assertEquals(1, lb.load("a"));
    }

    @Test
    void zeroCapacityServerNeverUsed() {
        LoadBalancer lb = withCapacities("a", 0, "b", 1);
        assertEquals("b", lb.connect("1", null));
        assertEquals(REJECTED, lb.connect("2", null));
    }

    // ---- Part 3 ----
    @Test
    void stickyUserReturnsToSameServer() {
        LoadBalancer lb = withCapacities("a", 3, "b", 3);
        assertEquals("a", lb.connect("1", "alice"));
        assertEquals("b", lb.connect("2", "bob"));
        assertEquals("a", lb.connect("3", "alice"));   // sticky, even though b is less loaded
    }

    @Test
    void stickyFallsBackWhenServerFull() {
        LoadBalancer lb = withCapacities("a", 1, "b", 2);
        assertEquals("a", lb.connect("1", "alice"));
        assertEquals("b", lb.connect("2", "alice"));   // a full: fall back, new sticky = b
        lb.disconnect("1");
        assertEquals("b", lb.connect("3", "alice"));   // stays on b now
    }

    // ---- Part 4 ----
    @Test
    void shutdownReroutesInOpenOrder() {
        LoadBalancer lb = withCapacities("a", 2, "b", 1, "c", 2);
        lb.connect("1", "u1");   // a
        lb.connect("2", "u2");   // b
        lb.connect("3", "u3");   // c
        lb.connect("4", "u4");   // a
        Map<String, String> moved = lb.shutdown("a");
        assertEquals(List.of("1", "4"), List.copyOf(moved.keySet()));
        assertEquals("c", moved.get("1"));
        assertEquals(REJECTED, moved.get("4"));        // b and c are now full
        assertEquals(0, lb.load("a"));
    }

    @Test
    void shutdownClearsStickinessToDeadServer() {
        LoadBalancer lb = withCapacities("a", 5, "b", 5);
        lb.connect("1", "alice");                      // a
        lb.shutdown("a");                              // alice's conn moves to b
        assertEquals("b", lb.connect("2", "alice"));
    }

    @Test
    void unknownServerShutdownIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> withCapacities("a", 1).shutdown("zzz"));
    }
}
