package com.hedgefund.treasury.config;

import com.hedgefund.ingest.config.IngestConfig;
import com.hedgefund.ingest.config.IngestConfigLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record TreasuryConfig(
    String baseUrl,
    List<String> endpoints,
    IngestConfig ingestConfig
) {
    public static TreasuryConfig fromYaml(Path path) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(Files.newInputStream(path));
        Map<String, Object> m = (Map<String, Object>) root.get("treasury");
        if (m == null) m = Map.of();

        String baseUrl = (String) m.getOrDefault("baseUrl", "https://home.treasury.gov");
        List<String> endpoints = (List<String>) m.getOrDefault("endpoints",
            m.getOrDefault("symbols",
                m.getOrDefault("series",
                    m.getOrDefault("tickers",
                        m.getOrDefault("protocols", List.of("treasury"))))));

        IngestConfig ingest = IngestConfigLoader.loadFromYaml(path, "treasury");
        return new TreasuryConfig(baseUrl, endpoints, ingest);
    }

    public static TreasuryConfig defaults() {
        return new TreasuryConfig(
            "https://home.treasury.gov",
            List.of("treasury"),
            IngestConfig.defaults("treasury")
        );
    }

    public List<String> effectiveKeys() {
        return endpoints != null && !endpoints.isEmpty() ? endpoints : ingestConfig().symbols();
    }
}
