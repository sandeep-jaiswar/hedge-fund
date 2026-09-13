package com.hedgefund.eastmoney.config;

import com.hedgefund.ingest.config.IngestConfig;
import com.hedgefund.ingest.config.IngestConfigLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record EastmoneyConfig(
    String baseUrl,
    List<String> symbols,
    List<String> series,
    List<String> tickers,
    List<String> protocols,
    String interval,
    int limit,
    IngestConfig ingestConfig
) {
    public static EastmoneyConfig fromYaml(Path path) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(Files.newInputStream(path));
        Map<String, Object> m = (Map<String, Object>) root.get("eastmoney");
        if (m == null) m = Map.of();

        String baseUrl = (String) m.getOrDefault("baseUrl", "https://push2.eastmoney.com");
        List<String> symbols = (List<String>) m.getOrDefault("symbols",
            m.getOrDefault("series",
                m.getOrDefault("tickers",
                    m.getOrDefault("protocols", List.of("eastmoney")))));
        List<String> series = (List<String>) m.getOrDefault("series", symbols);
        List<String> tickers = (List<String>) m.getOrDefault("tickers", symbols);
        List<String> protocols = (List<String>) m.getOrDefault("protocols", symbols);
        String interval = (String) m.getOrDefault("interval", "1d");
        int limit = ((Number) m.getOrDefault("limit", 30)).intValue();

        IngestConfig ingest = IngestConfigLoader.loadFromYaml(path, "eastmoney");
        return new EastmoneyConfig(baseUrl, symbols, series, tickers, protocols, interval, limit, ingest);
    }

    public static EastmoneyConfig defaults() {
        return new EastmoneyConfig(
            "https://push2.eastmoney.com",
            List.of("eastmoney"),
            List.of("eastmoney"),
            List.of("eastmoney"),
            List.of("eastmoney"),
            "1d",
            30,
            IngestConfig.defaults("eastmoney")
        );
    }

    public List<String> effectiveKeys() {
        return ingestConfig.resolveSymbols(symbols);
    }
}
