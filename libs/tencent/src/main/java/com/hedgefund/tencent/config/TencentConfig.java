package com.hedgefund.tencent.config;
import com.hedgefund.ingest.config.IngestConfig;
import com.hedgefund.ingest.config.IngestConfigLoader;
import org.yaml.snakeyaml.Yaml;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record TencentConfig(String baseUrl, List<String> symbols, IngestConfig ingestConfig) {
    @SuppressWarnings("unchecked")
    public static TencentConfig fromYaml(Path path) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(Files.newInputStream(path));
        Map<String, Object> m = (Map<String, Object>) root.get("tencent");
        if (m == null) m = Map.of();
        String baseUrl = (String) m.getOrDefault("baseUrl", "https://qt.gtimg.cn");
        List<String> symbols = (List<String>) m.getOrDefault("symbols", List.of());
        IngestConfig ingest = IngestConfigLoader.loadFromYaml(path, "tencent");
        return new TencentConfig(baseUrl, symbols, ingest);
    }
    public static TencentConfig defaults() {
        return new TencentConfig("https://qt.gtimg.cn", List.of(), IngestConfig.defaults("tencent"));
    }
}
