package com.practice.bank;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Problem 8: design an API to support transactions.
 * Part 1: create a payment; the same idempotency key returns the same payment
 *         instead of charging twice (safe client retries).
 * Part 2: authorize / capture / cancel through a state machine that rejects
 *         invalid transitions.
 * Part 3: partial refunds that can never exceed the captured amount.
 */
public class PaymentService {

    public enum Status { CREATED, AUTHORIZED, CAPTURED, CANCELED, REFUNDED }

    public static class PaymentException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public PaymentException(String message) { super(message); }
    }

    public static final class Payment {
        public final String id;
        public final long amountCents;
        public final String currency;
        private Status status = Status.CREATED;
        private long refundedCents = 0;

        Payment(String id, long amountCents, String currency) {
            this.id = id;
            this.amountCents = amountCents;
            this.currency = currency;
        }

        public Status status() { return status; }
        public long refundedCents() { return refundedCents; }
    }

    private static final Map<Status, Set<Status>> VALID = Map.of(
            Status.CREATED, Set.of(Status.AUTHORIZED, Status.CANCELED),
            Status.AUTHORIZED, Set.of(Status.CAPTURED, Status.CANCELED));

    private final Map<String, Payment> payments = new HashMap<>();
    private final Map<String, String> idempotencyKeys = new HashMap<>();
    private int nextId = 1;

    public Payment create(long amountCents, String currency, String idempotencyKey) {
        String existing = idempotencyKeys.get(idempotencyKey);
        if (existing != null) {
            Payment p = payments.get(existing);
            if (p.amountCents != amountCents || !p.currency.equalsIgnoreCase(currency)) {
                throw new PaymentException("idempotency key reused with different parameters");
            }
            return p;
        }
        if (amountCents <= 0) throw new PaymentException("amount must be positive");
        Payment p = new Payment("pay_" + nextId++, amountCents, currency.toLowerCase());
        payments.put(p.id, p);
        idempotencyKeys.put(idempotencyKey, p.id);
        return p;
    }

    public Payment authorize(String id) { return move(id, Status.AUTHORIZED); }
    public Payment capture(String id) { return move(id, Status.CAPTURED); }
    public Payment cancel(String id) { return move(id, Status.CANCELED); }

    public Payment refund(String id, long cents) {
        Payment p = get(id);
        if (p.status != Status.CAPTURED) throw new PaymentException("only captured payments can be refunded");
        if (cents <= 0 || p.refundedCents + cents > p.amountCents) {
            throw new PaymentException("refund exceeds captured amount");
        }
        p.refundedCents += cents;
        if (p.refundedCents == p.amountCents) p.status = Status.REFUNDED;
        return p;
    }

    private Payment move(String id, Status target) {
        Payment p = get(id);
        if (!VALID.getOrDefault(p.status, Set.of()).contains(target)) {
            throw new PaymentException("cannot go from " + p.status + " to " + target);
        }
        p.status = target;
        return p;
    }

    private Payment get(String id) {
        Payment p = payments.get(id);
        if (p == null) throw new PaymentException("no such payment: " + id);
        return p;
    }
}
