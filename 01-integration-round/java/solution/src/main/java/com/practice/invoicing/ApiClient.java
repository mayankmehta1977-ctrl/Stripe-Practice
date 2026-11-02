package com.practice.invoicing;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Client for the Ledger API (reference solution). */
public class ApiClient implements LedgerApi {

    /** Injectable so tests can record waits instead of actually sleeping. */
    @FunctionalInterface
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    static final int MAX_ATTEMPTS = 3;
    private static final Gson GSON = new Gson();

    private final String baseUrl;
    private final String apiKey;
    private final HttpTransport transport;
    private final Sleeper sleeper;
    private Map<String, Double> cachedRates;

    public ApiClient() {
        this(Config.API_BASE, Config.API_KEY, HttpTransport.jdk(), Thread::sleep);
    }

    public ApiClient(String baseUrl, String apiKey, HttpTransport transport, Sleeper sleeper) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.transport = transport;
        this.sleeper = sleeper;
    }

    // ---- endpoints -------------------------------------------------------

    @Override
    public Map<String, Double> getRates() {
        if (cachedRates == null) {
            JsonObject body = getJson("/v1/rates", Map.of("base", "USD"));
            cachedRates = GSON.fromJson(body.get("rates"), new TypeToken<Map<String, Double>>() {}.getType());
        }
        return cachedRates;
    }

    @Override
    public List<Customer> listCustomers() {
        List<Customer> all = new ArrayList<>();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("limit", "100");
        while (true) {
            JsonObject page = getJson("/v1/customers", params);
            List<Customer> data = GSON.fromJson(page.get("data"), new TypeToken<List<Customer>>() {}.getType());
            all.addAll(data);
            if (!page.get("has_more").getAsBoolean() || data.isEmpty()) {
                return all;
            }
            params.put("starting_after", data.get(data.size() - 1).id());
        }
    }

    // ---- HTTP with retries -----------------------------------------------

    JsonObject getJson(String path, Map<String, String> params) {
        URI uri = URI.create(baseUrl + path + toQuery(params));
        Map<String, String> headers = Map.of("Authorization", "Bearer " + apiKey);
        for (int attempt = 1; ; attempt++) {
            HttpTransport.Response resp;
            try {
                resp = transport.get(uri, headers);
            } catch (IOException e) {
                throw new ApiException(-1, "network error: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ApiException(-1, "interrupted");
            }
            if (resp.status() == 429 && attempt < MAX_ATTEMPTS) {
                long seconds = resp.header("Retry-After").map(Long::parseLong).orElse(1L);
                try {
                    sleeper.sleep(seconds * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ApiException(429, "interrupted while waiting to retry");
                }
                continue;
            }
            if (resp.status() >= 400) {
                throw new ApiException(resp.status(), errorMessage(resp.body()));
            }
            return GSON.fromJson(resp.body(), JsonObject.class);
        }
    }

    private static String errorMessage(String body) {
        try {
            return GSON.fromJson(body, JsonObject.class)
                    .getAsJsonObject("error").get("message").getAsString();
        } catch (JsonParseException | NullPointerException | IllegalStateException e) {
            return body;
        }
    }

    private static String toQuery(Map<String, String> params) {
        if (params.isEmpty()) return "";
        return "?" + params.entrySet().stream()
                .map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
