package com.hedgefund.ingest.client;

import com.hedgefund.ingest.config.IngestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public abstract class AbstractHttpClient {

    private static final Logger log = LoggerFactory.getLogger(AbstractHttpClient.class);

    protected final IngestConfig config;
    protected final HttpClient http;
    protected long lastRequestAt = 0;

    /** Shared connection pool: one HttpClient for all sources (HTTP/1.1 keep-alive + virtual threads). */
    private static final HttpClient SHARED = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .executor(Executors.newVirtualThreadPerTaskExecutor())
        .build();

    protected AbstractHttpClient(IngestConfig config) {
        this.config = config;
        this.http = SHARED;
    }

    protected synchronized void throttle() throws InterruptedException {
        double qps = config.rateLimit().qps();
        long minGapMs = (long) (1000 / qps);
        long now = System.currentTimeMillis();
        long gap = now - lastRequestAt;
        if (gap < minGapMs) {
            Thread.sleep(minGapMs - gap);
        }
        lastRequestAt = System.currentTimeMillis();
    }

    protected String fetchWithRetry(String url) throws Exception {
        int attempts = 0;
        long backoff = config.retry().backoffMs();

        while (true) {
            attempts++;
            try {
                throttle();
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Accept", "application/json")
                    .build();
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
