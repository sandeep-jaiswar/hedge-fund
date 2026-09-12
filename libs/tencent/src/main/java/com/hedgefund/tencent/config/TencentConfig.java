package com.hedgefund.tencent.config;

import com.hedgefund.ingest.config.IngestConfig;
import com.hedgefund.ingest.config.IngestConfigLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record TencentConfig(
    String baseUrl,
    List<String> symbols,
    List<String> series,
    List<String> tickers,
    List<String> protocols,
    String interval,
    int limit,
    IngestConfig ingestConfig
) {
    public static TencentConfig fromYaml(Path path) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(Files.newInputStream(path));
        Map<String, Object> m = (Map<String, Object>) root.get("tencent");
        if (m == null) m = Map.of();

        String baseUrl = (String) m.getOrDefault("baseUrl", "https://qt.gtimg.cn");
        List<String> symbols = (List<String>) m.getOrDefault("symbols",
            m.getOrDefault("series",
                m.getOrDefault("tickers",
                    m.getOrDefault("protocols", List.of("tencent")))));
        List<String> series = (List<String>) m.getOrDefault("series", symbols);
        List<String> tickers = (List<String>) m.getOrDefault("tickers", symbols);
        List<String> protocols = (List<String>) m.getOrDefault("protocols", symbols);
        String interval = (String) m.getOrDefault("interval", "1d");
        int limit = ((Number) m.getOrDefault("limit", 30)).intValue();

        IngestConfig ingest = IngestConfigLoader.loadFromYaml(path, "tencent");
        return new TencentConfig(baseUrl, symbols, series, tickers, protocols, interval, limit, ingest);
    }

    public static TencentConfig defaults() {
        return new TencentConfig(
            "https://qt.gtimg.cn",
            List.of("tencent"),
            List.of("tencent"),
            List.of("tencent"),
            List.of("tencent"),
            "1d",
            30,
            IngestConfig.defaults("tencent")
        );
    }

    public List<String> effectiveKeys() {
        if (!series.isEmpty() && !series.get(0).equals("tencent")) return series;
        if (!tickers.isEmpty() && !tickers.get(0).equals("tencent")) return tickers;
        if (!protocols.isEmpty() && !protocols.get(0).equals("tencent")) return protocols;
        return symbols;
    }
}
