package com.hedgefund.fred.config;

import com.hedgefund.ingest.config.IngestConfig;
import com.hedgefund.ingest.config.IngestConfigLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record FredConfig(
    String baseUrl,
    String apiKey,
    List<String> series,
    String interval,
    int limit,
    IngestConfig ingestConfig
) {
    public static FredConfig fromYaml(Path path) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(Files.newInputStream(path));
        Map<String, Object> m = (Map<String, Object>) root.get("fred");
        if (m == null) m = Map.of();

        String baseUrl = (String) m.getOrDefault("baseUrl", "https://fred.stlouisfed.org");
        String apiKey = (String) m.getOrDefault("apiKey", "");
        List<String> series = (List<String>) m.getOrDefault("series",
            m.getOrDefault("tickers",
                m.getOrDefault("protocols",
                    m.getOrDefault("symbols", List.of("DGS10")))));
        String interval = (String) m.getOrDefault("interval", "1d");
        int limit = ((Number) m.getOrDefault("limit", 30)).intValue();

        IngestConfig ingest = IngestConfigLoader.loadFromYaml(path, "fred");
        return new FredConfig(baseUrl, apiKey, series, interval, limit, ingest);
    }

    public static FredConfig defaults() {
        return new FredConfig(
            "https://fred.stlouisfed.org",
            "",
            List.of("DGS10"),
            "1d",
            30,
            IngestConfig.defaults("fred")
        );
    }

    public List<String> effectiveKeys() {
        return series != null && !series.isEmpty() ? series : ingestConfig().symbols();
    }
}
