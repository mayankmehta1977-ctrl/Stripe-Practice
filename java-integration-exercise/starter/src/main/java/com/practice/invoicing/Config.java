package com.practice.invoicing;

public final class Config {
    private Config() {}

    public static final String API_BASE = System.getenv().getOrDefault("LEDGER_API_BASE", "http://localhost:8765");
    public static final String API_KEY = System.getenv().getOrDefault("LEDGER_API_KEY", "sk_test_practice");
}
