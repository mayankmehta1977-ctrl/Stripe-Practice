package com.practice.ai.shipping;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShippingNetworkTest {
    static final String INPUT =
            "US:UK:UPS:20, US:CA:FedEx:3, CA:UK:DHL:7, US:UK:DHL:15, UK:DE:DHL:4, CA:FR:UPS:2, FR:DE:FedEx:1, CA:DE:UPS:12";
    final ShippingNetwork net = new ShippingNetwork(INPUT);

    static int cost(Optional<ShippingNetwork.Route> r) {
        return r.orElseThrow().cost();
    }

    // ---- Part 1 ----
    @Test
    void directPicksCheapestCarrierOnLane() {
        assertEquals(15, cost(net.direct("US", "UK")));
        assertEquals("DHL", net.direct("US", "UK").orElseThrow().legs().get(0).carrier());
    }

    @Test
    void directMissingLaneIsEmptyAndLanesAreOneWay() {
        assertTrue(net.direct("UK", "US").isEmpty());
        assertTrue(net.direct("US", "DE").isEmpty());
    }

    @Test
    void malformedInputIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ShippingNetwork("US:UK:UPS"));
        assertThrows(IllegalArgumentException.class, () -> new ShippingNetwork("US:UK:UPS:abc"));
        assertThrows(IllegalArgumentException.class, () -> new ShippingNetwork("US:UK:UPS:-1"));
    }

    // ---- Part 2 ----
    @Test
    void oneStopBeatsDirectWhenCheaper() {
        assertEquals(10, cost(net.withAtMostOneStop("US", "UK")));   // US->CA->UK = 3 + 7
    }

    @Test
    void oneStopReachesWhatDirectCannot() {
        assertEquals(15, cost(net.withAtMostOneStop("US", "DE")));   // US->CA->DE = 3 + 12
    }

    // ---- Part 3 ----
    @Test
    void cheapestUsesAnyNumberOfStops() {
        ShippingNetwork.Route r = net.cheapest("US", "DE").orElseThrow();
        assertEquals(6, r.cost());                                    // US->CA->FR->DE = 3+2+1
        assertEquals("US -(FedEx)-> CA -(UPS)-> FR -(FedEx)-> DE = 6", r.toString());
    }

    @Test
    void unreachableAndSameCountry() {
        assertTrue(net.cheapest("DE", "US").isEmpty());
        assertEquals(0, cost(net.cheapest("US", "US")));
    }

    // ---- Part 4 ----
    @Test
    void legLimitChangesTheAnswer() {
        assertEquals(6, cost(net.cheapest("US", "DE", 3, Set.of())));
        // best with 2 legs: US->CA->DE = 3 + 12 (US->UK->DE = 15 + 4 is dearer)
        assertEquals(15, cost(net.cheapest("US", "DE", 2, Set.of())));
        assertTrue(net.cheapest("US", "DE", 1, Set.of()).isEmpty());
    }

    @Test
    void excludedCarriersAreAvoided() {
        // without FedEx: US->UK(DHL 15)->DE(4) = 19
        assertEquals(19, cost(net.cheapest("US", "DE", 5, ShippingNetwork.carriers("FedEx"))));
        assertTrue(net.cheapest("US", "CA", 5, ShippingNetwork.carriers("FedEx")).isEmpty());
    }

    @Test
    void zeroLegsOnlyReachesStart() {
        assertTrue(net.cheapest("US", "UK", 0, Set.of()).isEmpty());
    }
}
