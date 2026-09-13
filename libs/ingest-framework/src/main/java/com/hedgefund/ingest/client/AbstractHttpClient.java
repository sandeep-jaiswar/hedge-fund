package com.hedgefund.ingest.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.hedgefund.common.Json;
import com.hedgefund.ingest.config.IngestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public abstract class AbstractHttpClient {

    private static final Logger log = LoggerFactory.getLogger(AbstractHttpClient.class);

    protected final IngestConfig config;
    protected final HttpClient http;

    /** Shared rate limiters per base URL — prevents 22 sources from overwhelming a single API. */
    private static final ConcurrentHashMap<String, long[]> RATE_LIMITERS = new ConcurrentHashMap<>();

    /** Shared connection pool: one HttpClient for all sources (HTTP/1.1 keep-alive + virtual threads). */
    private static final HttpClient SHARED = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .executor(Executors.newVirtualThreadPerTaskExecutor())
        .build();

    protected AbstractHttpClient(IngestConfig config) {
        this.config = config;
        this.http = SHARED;
    }

    /** Override to add source-specific headers (e.g., Referer for Sina). */
    protected Map<String, String> defaultHeaders() {
        return Collections.emptyMap();
    }

    protected void throttle() throws InterruptedException {
        String baseUrl = config.baseUrl();
        double qps = config.rateLimit().qps();
        long minGapMs = (long) (1000 / qps);
        long[] lastRequest = RATE_LIMITERS.computeIfAbsent(baseUrl, k -> new long[]{0});
        synchronized (lastRequest) {
            long now = System.currentTimeMillis();
            long gap = now - lastRequest[0];
            if (gap < minGapMs) {
                Thread.sleep(minGapMs - gap);
            }
            lastRequest[0] = System.currentTimeMillis();
        }
    }

    public String fetchRaw(String url) throws Exception {
        return fetchWithRetry(url);
    }

    public JsonNode fetchJson(String url) throws Exception {
        String raw = fetchWithRetry(url);
        return Json.shared().readTree(raw);
    }

    protected String fetchWithRetry(String url) throws Exception {
        int attempts = 0;
        long backoff = config.retry().backoffMs();

        while (true) {
            attempts++;
            try {
                throttle();
                HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/json,*/*");
                for (Map.Entry<String, String> h : defaultHeaders().entrySet()) {
                    builder.header(h.getKey(), h.getValue());
                }
                HttpRequest request = builder.build();
                HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() == 429 || resp.statusCode() >= 500) {
                    throw new IOException("HTTP " + resp.statusCode() + " " +
                        resp.body().substring(0, Math.min(200, resp.body().length())));
                }
                if (resp.statusCode() != 200) {
                    throw new IOException("HTTP " + resp.statusCode() + " " +
                        resp.body().substring(0, Math.min(400, resp.body().length())));
                }
                return resp.body();
            } catch (Exception e) {
                if (attempts >= config.retry().maxAttempts()) throw e;
                log.warn("Fetch failed {}/{} {} -> retry {}ms {}",
                    attempts, config.retry().maxAttempts(), url, backoff, e.toString());
                Thread.sleep(backoff + ThreadLocalRandom.current().nextInt(200));
                backoff = Math.min(backoff * 2, config.retry().maxBackoffMs());
            }
        }
    }
}
