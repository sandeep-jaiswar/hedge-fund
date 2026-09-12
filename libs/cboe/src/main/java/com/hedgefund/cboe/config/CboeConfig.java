package com.hedgefund.cboe.config;
import com.hedgefund.ingest.config.IngestConfig;
import com.hedgefund.ingest.config.IngestConfigLoader;
import org.yaml.snakeyaml.Yaml;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record CboeConfig(String baseUrl, List<String> symbols, IngestConfig ingestConfig) {
    @SuppressWarnings("unchecked")
    public static CboeConfig fromYaml(Path path) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(Files.newInputStream(path));
        Map<String, Object> m = (Map<String, Object>) root.get("cboe");
        if (m == null) m = Map.of();
        String baseUrl = (String) m.getOrDefault("baseUrl", "https://cdn.cboe.com");
        List<String> symbols = (List<String>) m.getOrDefault("symbols", List.of());
        IngestConfig ingest = IngestConfigLoader.loadFromYaml(path, "cboe");
        return new CboeConfig(baseUrl, symbols, ingest);
    }
    public static CboeConfig defaults() {
        return new CboeConfig("https://cdn.cboe.com", List.of(), IngestConfig.defaults("cboe"));
    }
}
