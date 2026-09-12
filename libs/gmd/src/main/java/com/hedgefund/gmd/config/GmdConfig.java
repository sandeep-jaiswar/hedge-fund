package com.hedgefund.gmd.config;

import com.hedgefund.ingest.config.IngestConfig;
import com.hedgefund.ingest.config.IngestConfigLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record GmdConfig(
    String baseUrl,
    List<String> series,
    List<String> tickers,
    List<String> protocols,
    String interval,
    int limit,
    IngestConfig ingestConfig
) {

    @SuppressWarnings("unchecked")
    public static GmdConfig fromYaml(Path path) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(Files.newInputStream(path));
        Map<String, Object> m = (Map<String, Object>) root.get("gmd");
        if (m == null) m = Map.of();

        String baseUrl = (String) m.getOrDefault("baseUrl", "https://raw.githubusercontent.com");
        List<String> symbols = (List<String>) m.getOrDefault("symbols",
            m.getOrDefault("series",
                m.getOrDefault("tickers",
                    m.getOrDefault("protocols", List.of("gmd")))));
        List<String> series = (List<String>) m.getOrDefault("series", symbols);
        List<String> tickers = (List<String>) m.getOrDefault("tickers", symbols);
        List<String> protocols = (List<String>) m.getOrDefault("protocols", symbols);
        String interval = (String) m.getOrDefault("interval", "1d");
        int limit = ((Number) m.getOrDefault("limit", 30)).intValue();

        IngestConfig ingest = IngestConfigLoader.loadFromYaml(path, "gmd");
        IngestConfig resolved = new IngestConfig(
            ingest.sourceId(),
            baseUrl,
            symbols,
            ingest.concurrency(),
            ingest.retry(),
            ingest.rateLimit(),
            ingest.paths()
        );

        return new GmdConfig(baseUrl, series, tickers, protocols, interval, limit, resolved);
    }

    public static GmdConfig defaults() {
        return new GmdConfig(
            "https://raw.githubusercontent.com",
            List.of("gmd"),
            List.of("gmd"),
            List.of("gmd"),
            "1d",
            30,
            IngestConfig.defaults("gmd")
        );
    }

    public List<String> effectiveKeys() {
        if (series != null && !series.isEmpty() && !series.get(0).equals("gmd")) return series;
        if (tickers != null && !tickers.isEmpty() && !tickers.get(0).equals("gmd")) return tickers;
        if (protocols != null && !protocols.isEmpty() && !protocols.get(0).equals("gmd")) return protocols;
        return ingestConfig().symbols();
    }

    public Retry retry() { return new Retry(ingestConfig().retry().maxAttempts(), ingestConfig().retry().backoffMs(), ingestConfig().retry().maxBackoffMs()); }
    public RateLimit rateLimit() { return new RateLimit(ingestConfig().rateLimit().qps(), ingestConfig().rateLimit().burst()); }
    public Paths paths() { return new Paths(ingestConfig().paths().bronze(), ingestConfig().paths().silver(), ingestConfig().paths().catalog()); }
    public int concurrency() { return ingestConfig().concurrency(); }

    public record Retry(int maxAttempts, long backoffMs, long maxBackoffMs) {}
    public record RateLimit(double qps, int burst) {}
    public record Paths(String bronze, String silver, String catalog) {}
}
