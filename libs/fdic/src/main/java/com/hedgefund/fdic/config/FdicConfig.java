package com.hedgefund.fdic.config;

import com.hedgefund.ingest.config.IngestConfig;
import com.hedgefund.ingest.config.IngestConfigLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record FdicConfig(String baseUrl, List<String> series, IngestConfig ingestConfig) {

    @SuppressWarnings("unchecked")
    public static FdicConfig fromYaml(Path path) throws IOException {
        IngestConfig ingest = IngestConfigLoader.loadFromYaml(path, "fdic");
        Yaml yaml = new Yaml();
        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(path)) {
            root = yaml.load(in);
        }
        Map<String, Object> m = (Map<String, Object>) root.getOrDefault("fdic", Map.of());
        List<String> series = (List<String>) m.getOrDefault("series", ingest.symbols());
        return new FdicConfig(ingest.baseUrl(), series, ingest);
    }

    public static FdicConfig defaults() {
        return new FdicConfig("https://www.fdic.gov", List.of(), IngestConfig.defaults("fdic"));
    }

    public List<String> effectiveKeys() {
        return ingestConfig.resolveSymbols(series);
    }
}
