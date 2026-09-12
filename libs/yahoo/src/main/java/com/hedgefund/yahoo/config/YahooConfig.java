package com.hedgefund.yahoo.config;

import com.hedgefund.ingest.config.IngestConfig;
import com.hedgefund.ingest.config.IngestConfigLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record YahooConfig(
    String baseUrl,
    List<String> symbols,
    String interval,
    String range,
    IngestConfig ingestConfig
) {
    public static YahooConfig fromYaml(Path path) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(Files.newInputStream(path));
        Map<String, Object> y = (Map<String, Object>) root.get("yahoo");

        String baseUrl = (String) y.getOrDefault("baseUrl", "https://query1.finance.yahoo.com");
        List<String> symbols = (List<String>) y.getOrDefault("symbols", List.of("AAPL"));
        String interval = (String) y.getOrDefault("interval", "1d");
        String range = (String) y.getOrDefault("range", "1mo");

        IngestConfig ingest = IngestConfigLoader.loadFromYaml(path, "yahoo");
        return new YahooConfig(baseUrl, symbols, interval, range, ingest);
    }

    public static YahooConfig defaults() {
        IngestConfig ingest = IngestConfig.defaults("yahoo");
        return new YahooConfig(
            "https://query1.finance.yahoo.com",
            List.of("AAPL"),
            "1d",
            "1mo",
            ingest
        );
    }
}
