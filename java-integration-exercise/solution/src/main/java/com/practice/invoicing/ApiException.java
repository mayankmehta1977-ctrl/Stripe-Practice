package com.practice.invoicing;

/** A non-retryable Ledger API failure, carrying the server's error message. */
public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String message) {
        super("Ledger API error " + status + ": " + message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
