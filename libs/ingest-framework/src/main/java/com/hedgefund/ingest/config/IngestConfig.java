package com.hedgefund.ingest.config;

import java.util.List;

public record IngestConfig(
    String sourceId,
    String baseUrl,
    List<String> symbols,
    int concurrency,
    Retry retry,
    RateLimit rateLimit,
    Paths paths
) {
    public record Retry(int maxAttempts, long backoffMs, long maxBackoffMs) {}
    public record RateLimit(double qps, int burst) {}
    public record Paths(String bronze, String silver, String catalog) {}

    public static IngestConfig defaults(String sourceId) {
        return new IngestConfig(
            sourceId,
            "https://localhost",
            List.of(),
            4,
            new Retry(3, 800, 8000),
            new RateLimit(2, 4),
            new Paths("data/bronze/" + sourceId, "data/silver/" + sourceId, "catalog/glue.json")
        );
    }
}
