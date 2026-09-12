package com.hedgefund.coinbase.config;
import com.hedgefund.ingest.config.IngestConfig;
import com.hedgefund.ingest.config.IngestConfigLoader;
import org.yaml.snakeyaml.Yaml;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record CoinbaseConfig(String baseUrl, List<String> symbols, IngestConfig ingestConfig) {
    @SuppressWarnings("unchecked")
    public static CoinbaseConfig fromYaml(Path path) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(Files.newInputStream(path));
        Map<String, Object> m = (Map<String, Object>) root.get("coinbase");
        if (m == null) m = Map.of();
        String baseUrl = (String) m.getOrDefault("baseUrl", "https://api.coinbase.com");
        List<String> symbols = (List<String>) m.getOrDefault("symbols", List.of());
        IngestConfig ingest = IngestConfigLoader.loadFromYaml(path, "coinbase");
        return new CoinbaseConfig(baseUrl, symbols, ingest);
    }
    public static CoinbaseConfig defaults() {
        return new CoinbaseConfig("https://api.coinbase.com", List.of(), IngestConfig.defaults("coinbase"));
    }
}
