package com.hedgefund.defillama.config;
import com.hedgefund.ingest.config.IngestConfig;
import com.hedgefund.ingest.config.IngestConfigLoader;
import org.yaml.snakeyaml.Yaml;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record DefillamaConfig(String baseUrl, List<String> symbols, IngestConfig ingestConfig) {
    @SuppressWarnings("unchecked")
    public static DefillamaConfig fromYaml(Path path) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(Files.newInputStream(path));
        Map<String, Object> m = (Map<String, Object>) root.get("defillama");
        if (m == null) m = Map.of();
        String baseUrl = (String) m.getOrDefault("baseUrl", "https://api.llama.fi");
        List<String> symbols = (List<String>) m.getOrDefault("symbols", List.of());
        IngestConfig ingest = IngestConfigLoader.loadFromYaml(path, "defillama");
        return new DefillamaConfig(baseUrl, symbols, ingest);
    }
    public static DefillamaConfig defaults() {
        return new DefillamaConfig("https://api.llama.fi", List.of(), IngestConfig.defaults("defillama"));
    }
}
