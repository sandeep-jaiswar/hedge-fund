package com.hedgefund.bls.config;

import com.hedgefund.ingest.config.IngestConfig;
import com.hedgefund.ingest.config.IngestConfigLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record BlsConfig(String baseUrl, String apiKey, List<String> series, IngestConfig ingestConfig) {

    @SuppressWarnings("unchecked")
    public static BlsConfig fromYaml(Path path) throws IOException {
        IngestConfig ingest = IngestConfigLoader.loadFromYaml(path, "bls");
        Yaml yaml = new Yaml();
        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(path)) {
            root = yaml.load(in);
        }
        Map<String, Object> m = (Map<String, Object>) root.getOrDefault("bls", Map.of());
        String apiKey = (String) m.getOrDefault("apiKey", "");
        List<String> series = (List<String>) m.getOrDefault("series", ingest.symbols());
        return new BlsConfig(ingest.baseUrl(), apiKey, series, ingest);
    }

    public static BlsConfig defaults() {
        return new BlsConfig("https://download.bls.gov", "", List.of(), IngestConfig.defaults("bls"));
    }

    public List<String> effectiveKeys() {
        return ingestConfig.resolveSymbols(series);
    }
}
