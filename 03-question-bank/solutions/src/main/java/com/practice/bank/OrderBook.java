package com.practice.bank;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Problem 7: "array problem with custom classes" - a representative version.
 * Part 1: add buy/sell orders; report best bid (highest buy) and best ask (lowest sell).
 * Part 2: an incoming order that crosses the book trades against the best
 *         opposite orders (price, then arrival time); any rest stays in the book.
 */
public class OrderBook {

    public enum Side { BUY, SELL }

    public record Fill(long price, long quantity) {}

    private static final class Order {
        final long price;
        long quantity;
        final long seq;

        Order(long price, long quantity, long seq) {
            this.price = price;
            this.quantity = quantity;
            this.seq = seq;
        }
    }

    private final PriorityQueue<Order> bids = new PriorityQueue<>(
            Comparator.comparingLong((Order o) -> -o.price).thenComparingLong(o -> o.seq));
    private final PriorityQueue<Order> asks = new PriorityQueue<>(
            Comparator.comparingLong((Order o) -> o.price).thenComparingLong(o -> o.seq));
    private long seq = 0;

    public Optional<Long> bestBid() {
        return Optional.ofNullable(bids.peek()).map(o -> o.price);
    }

    public Optional<Long> bestAsk() {
        return Optional.ofNullable(asks.peek()).map(o -> o.price);
    }

    public List<Fill> add(Side side, long price, long quantity) {
        if (price <= 0 || quantity <= 0) throw new IllegalArgumentException("price and quantity must be positive");
        Order incoming = new Order(price, quantity, ++seq);
        PriorityQueue<Order> opposite = side == Side.BUY ? asks : bids;
        List<Fill> fills = new ArrayList<>();
        while (incoming.quantity > 0 && !opposite.isEmpty()) {
            Order top = opposite.peek();
            boolean crosses = side == Side.BUY ? price >= top.price : price <= top.price;
            if (!crosses) break;
            long traded = Math.min(incoming.quantity, top.quantity);
            fills.add(new Fill(top.price, traded));          // trades at the resting order's price
            incoming.quantity -= traded;
            top.quantity -= traded;
            if (top.quantity == 0) opposite.poll();
        }
        if (incoming.quantity > 0) (side == Side.BUY ? bids : asks).add(incoming);
        return fills;
    }
}
