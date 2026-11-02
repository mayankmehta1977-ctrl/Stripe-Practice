package com.practice.invoicing;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/** Thin seam over the JDK HTTP client so ApiClient can be unit-tested without a network. */
public interface HttpTransport {

    record Response(int status, Map<String, String> headers, String body) {
        Optional<String> header(String name) {
            return headers.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(name))
                    .map(Map.Entry::getValue)
                    .findFirst();
        }
    }

    Response get(URI uri, Map<String, String> headers) throws IOException, InterruptedException;

    /** Real implementation using java.net.http.HttpClient (JDK 11+). */
    static HttpTransport jdk() {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        return (uri, headers) -> {
            HttpRequest.Builder req = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET();
            headers.forEach(req::header);
            HttpResponse<String> resp = client.send(req.build(), HttpResponse.BodyHandlers.ofString());
            Map<String, String> flat = new java.util.HashMap<>();
            resp.headers().map().forEach((k, v) -> flat.put(k, v.isEmpty() ? "" : v.get(0)));
            return new Response(resp.statusCode(), flat, resp.body());
        };
    }
}
