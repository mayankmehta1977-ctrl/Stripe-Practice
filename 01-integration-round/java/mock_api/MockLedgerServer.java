/*
 * Local mock of the "Ledger API" used in this exercise. No dependencies.
 *
 * Run it in a separate terminal before you start (Java 17+):
 *     java mock_api/MockLedgerServer.java
 * It listens on http://localhost:8765. See API.md for the endpoints.
 */
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MockLedgerServer {
    static final String API_KEY = "sk_test_practice";
    static final String RATES =
            "{\"USD\": 1.0, \"EUR\": 0.92, \"GBP\": 0.79, \"INR\": 83.0, \"JPY\": 150.0}";
    static final List<String[]> CUSTOMERS = List.of(
            new String[]{"cus_001", "Acme Corp", "US"},
            new String[]{"cus_002", "Bharat Textiles", "IN"},
            new String[]{"cus_003", "Euro Parts GmbH", "DE"},
            new String[]{"cus_004", "Tokyo Gadgets", "JP"},
            new String[]{"cus_005", "London Books Ltd", "GB"});
    static final AtomicInteger REQUESTS = new AtomicInteger();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8765), 0);
        server.createContext("/", MockLedgerServer::handle);
        server.start();
        System.out.println("Ledger API mock running on http://localhost:8765  (Ctrl+C to stop)");
    }

    static void handle(HttpExchange ex) throws IOException {
        int n = REQUESTS.incrementAndGet();
        if (!("Bearer " + API_KEY).equals(ex.getRequestHeaders().getFirst("Authorization"))) {
            send(ex, 401, error("authentication_error", "Invalid API key"));
            return;
        }
        // Every 4th request is rate limited, to exercise retry logic (Part 3).
        if (n % 4 == 0) {
            ex.getResponseHeaders().add("Retry-After", "1");
            send(ex, 429, error("rate_limit_error", "Too many requests"));
            return;
        }
        String path = ex.getRequestURI().getPath();
        Map<String, String> q = query(ex.getRequestURI().getRawQuery());

        if (path.equals("/v1/rates")) {
            if (!q.getOrDefault("base", "USD").equalsIgnoreCase("USD")) {
                send(ex, 400, error("invalid_request_error", "Only base=USD is supported"));
                return;
            }
            send(ex, 200, "{\"object\": \"rates\", \"base\": \"USD\", \"rates\": " + RATES + "}");
            return;
        }
        if (path.equals("/v1/customers")) {
            int limit = Math.min(Integer.parseInt(q.getOrDefault("limit", "2")), 2); // server caps page size at 2
            int start = 0;
            if (q.containsKey("starting_after")) {
                start = -1;
                for (int i = 0; i < CUSTOMERS.size(); i++) {
                    if (CUSTOMERS.get(i)[0].equals(q.get("starting_after"))) start = i + 1;
                }
                if (start < 0) {
                    send(ex, 400, error("invalid_request_error", "Unknown cursor"));
                    return;
                }
            }
            int end = Math.min(start + limit, CUSTOMERS.size());
            StringBuilder data = new StringBuilder();
            for (int i = start; i < end; i++) {
                String[] c = CUSTOMERS.get(i);
                if (data.length() > 0) data.append(", ");
                data.append(String.format("{\"id\": \"%s\", \"name\": \"%s\", \"country\": \"%s\"}", c[0], c[1], c[2]));
            }
            send(ex, 200, "{\"object\": \"list\", \"data\": [" + data + "], \"has_more\": " + (end < CUSTOMERS.size()) + "}");
            return;
        }
        send(ex, 404, error("invalid_request_error", "Not found"));
    }

    static String error(String type, String message) {
        return "{\"error\": {\"type\": \"" + type + "\", \"message\": \"" + message + "\"}}";
    }

    static Map<String, String> query(String raw) {
        Map<String, String> out = new HashMap<>();
        if (raw == null) return out;
        for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            out.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return out;
    }

    static void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
