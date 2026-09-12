package com.hedgefund.calcfi.config;

import com.hedgefund.ingest.config.IngestConfig;
import com.hedgefund.ingest.config.IngestConfigLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record CalcfiConfig(String baseUrl, List<String> series, IngestConfig ingestConfig) {

    @SuppressWarnings("unchecked")
    public static CalcfiConfig fromYaml(Path path) throws IOException {
        IngestConfig ingest = IngestConfigLoader.loadFromYaml(path, "calcfi");
        Yaml yaml = new Yaml();
        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(path)) {
            root = yaml.load(in);
        }
        Map<String, Object> m = (Map<String, Object>) root.getOrDefault("calcfi", Map.of());
        List<String> series = (List<String>) m.getOrDefault("series", ingest.symbols());
        return new CalcfiConfig(ingest.baseUrl(), series, ingest);
    }

    public static CalcfiConfig defaults() {
        return new CalcfiConfig("https://raw.githubusercontent.com", List.of(), IngestConfig.defaults("calcfi"));
    }

    public List<String> effectiveKeys() {
        return series != null && !series.isEmpty() ? series : ingestConfig().symbols();
    }
}
