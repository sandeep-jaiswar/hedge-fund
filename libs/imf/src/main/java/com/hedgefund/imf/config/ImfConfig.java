package com.hedgefund.imf.config;

import com.hedgefund.ingest.config.IngestConfig;
import com.hedgefund.ingest.config.IngestConfigLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record ImfConfig(
    String baseUrl,
    List<String> series,
    List<String> tickers,
    List<String> protocols,
    String interval,
    int limit,
    IngestConfig ingestConfig
) {

    @SuppressWarnings("unchecked")
    public static ImfConfig fromYaml(Path path) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(Files.newInputStream(path));
        Map<String, Object> m = (Map<String, Object>) root.get("imf");
        if (m == null) m = Map.of();

        String baseUrl = (String) m.getOrDefault("baseUrl", "http://dataservices.imf.org");
        List<String> symbols = (List<String>) m.getOrDefault("symbols",
            m.getOrDefault("series",
                m.getOrDefault("tickers",
                    m.getOrDefault("protocols", List.of("imf")))));
        List<String> series = (List<String>) m.getOrDefault("series", symbols);
        List<String> tickers = (List<String>) m.getOrDefault("tickers", symbols);
        List<String> protocols = (List<String>) m.getOrDefault("protocols", symbols);
        String interval = (String) m.getOrDefault("interval", "1d");
        int limit = ((Number) m.getOrDefault("limit", 30)).intValue();

        IngestConfig ingest = IngestConfigLoader.loadFromYaml(path, "imf");
        IngestConfig resolved = new IngestConfig(
            ingest.sourceId(),
            baseUrl,
            symbols,
            ingest.concurrency(),
            ingest.retry(),
            ingest.rateLimit(),
            ingest.paths()
        );

        return new ImfConfig(baseUrl, series, tickers, protocols, interval, limit, resolved);
    }

    public static ImfConfig defaults() {
        return new ImfConfig(
            "http://dataservices.imf.org",
            List.of("imf"),
            List.of("imf"),
            List.of("imf"),
            "1d",
            30,
            IngestConfig.defaults("imf")
        );
    }

    public List<String> effectiveKeys() {
        return ingestConfig.resolveSymbols(series);
    }

}
