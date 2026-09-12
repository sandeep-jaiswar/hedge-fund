package com.hedgefund.eia.config;

import com.hedgefund.ingest.config.IngestConfig;
import com.hedgefund.ingest.config.IngestConfigLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record EiaConfig(String baseUrl, List<String> series, IngestConfig ingestConfig) {

    @SuppressWarnings("unchecked")
    public static EiaConfig fromYaml(Path path) throws IOException {
        IngestConfig ingest = IngestConfigLoader.loadFromYaml(path, "eia");
        Yaml yaml = new Yaml();
        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(path)) {
            root = yaml.load(in);
        }
        Map<String, Object> m = (Map<String, Object>) root.getOrDefault("eia", Map.of());
        List<String> series = (List<String>) m.getOrDefault("series", ingest.symbols());
        return new EiaConfig(ingest.baseUrl(), series, ingest);
    }

    public static EiaConfig defaults() {
        return new EiaConfig("https://www.eia.gov", List.of(), IngestConfig.defaults("eia"));
    }

    public List<String> effectiveKeys() {
        return series != null && !series.isEmpty() ? series : ingestConfig().symbols();
    }
}
