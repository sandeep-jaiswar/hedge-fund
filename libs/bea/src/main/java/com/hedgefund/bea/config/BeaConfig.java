package com.hedgefund.bea.config;

import com.hedgefund.ingest.config.IngestConfig;
import com.hedgefund.ingest.config.IngestConfigLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record BeaConfig(
    String baseUrl,
    List<String> series,
    List<String> tickers,
    List<String> protocols,
    String interval,
    int limit,
    IngestConfig ingestConfig
) {

    @SuppressWarnings("unchecked")
    public static BeaConfig fromYaml(Path path) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(Files.newInputStream(path));
        Map<String, Object> m = (Map<String, Object>) root.get("bea");
        if (m == null) m = Map.of();

        String baseUrl = (String) m.getOrDefault("baseUrl", "https://apps.bea.gov");
        List<String> symbols = (List<String>) m.getOrDefault("symbols",
            m.getOrDefault("series",
                m.getOrDefault("tickers",
                    m.getOrDefault("protocols", List.of("bea")))));
        List<String> series = (List<String>) m.getOrDefault("series", symbols);
        List<String> tickers = (List<String>) m.getOrDefault("tickers", symbols);
        List<String> protocols = (List<String>) m.getOrDefault("protocols", symbols);
        String interval = (String) m.getOrDefault("interval", "1d");
        int limit = ((Number) m.getOrDefault("limit", 30)).intValue();

        IngestConfig ingest = IngestConfigLoader.loadFromYaml(path, "bea");
        IngestConfig resolved = new IngestConfig(
            ingest.sourceId(),
            baseUrl,
            symbols,
            ingest.concurrency(),
            ingest.retry(),
            ingest.rateLimit(),
            ingest.paths()
        );

        return new BeaConfig(baseUrl, series, tickers, protocols, interval, limit, resolved);
    }

    public static BeaConfig defaults() {
        return new BeaConfig(
            "https://apps.bea.gov",
            List.of("bea"),
            List.of("bea"),
            List.of("bea"),
            "1d",
            30,
            IngestConfig.defaults("bea")
        );
    }

    public List<String> effectiveKeys() {
        return ingestConfig.resolveSymbols(series);
    }

}
