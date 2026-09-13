package com.hedgefund.binance.config;

import com.hedgefund.ingest.config.IngestConfig;
import com.hedgefund.ingest.config.IngestConfigLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record BinanceConfig(
    String baseUrl,
    List<String> symbols,
    String interval,
    int limit,
    IngestConfig ingestConfig
) {
    public static BinanceConfig fromYaml(Path path) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(Files.newInputStream(path));
        Map<String, Object> m = (Map<String, Object>) root.get("binance");
        if (m == null) m = Map.of();

        String baseUrl = (String) m.getOrDefault("baseUrl", "https://api.binance.com");
        List<String> symbols = (List<String>) m.getOrDefault("symbols",
            m.getOrDefault("series",
                m.getOrDefault("tickers",
                    m.getOrDefault("protocols", List.of("BTCUSDT")))));
        String interval = (String) m.getOrDefault("interval", "1d");
        int limit = ((Number) m.getOrDefault("limit", 30)).intValue();

        IngestConfig ingest = IngestConfigLoader.loadFromYaml(path, "binance");
        return new BinanceConfig(baseUrl, symbols, interval, limit, ingest);
    }

    public static BinanceConfig defaults() {
        return new BinanceConfig(
            "https://api.binance.com",
            List.of("BTCUSDT"),
            "1d",
            30,
            IngestConfig.defaults("binance")
        );
    }

    public List<String> effectiveKeys() {
        return ingestConfig.resolveSymbols(symbols);
    }
}
