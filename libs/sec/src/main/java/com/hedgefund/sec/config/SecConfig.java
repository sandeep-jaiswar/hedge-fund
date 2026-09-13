package com.hedgefund.sec.config;

import com.hedgefund.ingest.config.IngestConfig;
import com.hedgefund.ingest.config.IngestConfigLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record SecConfig(
    String baseUrl,
    List<String> tickers,
    String interval,
    int limit,
    IngestConfig ingestConfig
) {
    @SuppressWarnings("unchecked")
    public static SecConfig fromYaml(Path path) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(Files.newInputStream(path));
        Map<String, Object> m = (Map<String, Object>) root.get("sec");
        if (m == null) m = Map.of();

        String baseUrl = (String) m.getOrDefault("baseUrl", "https://www.sec.gov");
        List<String> tickers = (List<String>) m.getOrDefault("tickers",
            m.getOrDefault("symbols",
                m.getOrDefault("series",
                    m.getOrDefault("protocols", List.of("AAPL", "MSFT")))));
        String interval = (String) m.getOrDefault("interval", "1d");
        int limit = ((Number) m.getOrDefault("limit", 30)).intValue();

        IngestConfig ingest = IngestConfigLoader.loadFromYaml(path, "sec");
        IngestConfig resolved = new IngestConfig(
            ingest.sourceId(),
            baseUrl,
            tickers,
            ingest.concurrency(),
            ingest.retry(),
            ingest.rateLimit(),
            ingest.paths()
        );

        return new SecConfig(baseUrl, tickers, interval, limit, resolved);
    }

    public static SecConfig defaults() {
        return new SecConfig(
            "https://www.sec.gov",
            List.of("AAPL", "MSFT"),
            "1d",
            30,
            IngestConfig.defaults("sec")
        );
    }

    public List<String> effectiveKeys() {
        return ingestConfig.resolveSymbols(tickers);
    }

}
