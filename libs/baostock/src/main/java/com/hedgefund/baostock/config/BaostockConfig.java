package com.hedgefund.baostock.config;
import com.hedgefund.ingest.config.IngestConfig;
import com.hedgefund.ingest.config.IngestConfigLoader;
import org.yaml.snakeyaml.Yaml;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record BaostockConfig(String baseUrl, List<String> symbols, IngestConfig ingestConfig) {
    @SuppressWarnings("unchecked")
    public static BaostockConfig fromYaml(Path path) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(Files.newInputStream(path));
        Map<String, Object> m = (Map<String, Object>) root.get("baostock");
        if (m == null) m = Map.of();
        String baseUrl = (String) m.getOrDefault("baseUrl", "https://www.baostock.com");
        List<String> symbols = (List<String>) m.getOrDefault("symbols", List.of());
        IngestConfig ingest = IngestConfigLoader.loadFromYaml(path, "baostock");
        return new BaostockConfig(baseUrl, symbols, ingest);
    }
    public static BaostockConfig defaults() {
        return new BaostockConfig("https://www.baostock.com", List.of(), IngestConfig.defaults("baostock"));
    }
}
