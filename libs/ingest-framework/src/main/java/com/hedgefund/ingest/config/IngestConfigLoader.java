package com.hedgefund.ingest.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class IngestConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(IngestConfigLoader.class);

    private IngestConfigLoader() {}

    @SuppressWarnings("unchecked")
    public static IngestConfig loadFromYaml(Path path, String sourceId) throws IOException {
        if (!Files.exists(path)) {
            log.warn("Config file not found: {}, using defaults for {}", path, sourceId);
            return IngestConfig.defaults(sourceId);
        }

        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(Files.newInputStream(path));
        Map<String, Object> src = (Map<String, Object>) root.get(sourceId);

        if (src == null) {
            log.warn("Source '{}' not found in config file: {}, using defaults", sourceId, path);
            return IngestConfig.defaults(sourceId);
        }

        String baseUrl = (String) src.getOrDefault("baseUrl", "https://localhost");
        List<String> symbols = (List<String>) src.getOrDefault("symbols", List.of());

        Map<String, Object> retryMap = (Map<String, Object>) src.getOrDefault("retry", Map.of());
        IngestConfig.Retry retry = new IngestConfig.Retry(
            (int) retryMap.getOrDefault("maxAttempts", 3),
            ((Number) retryMap.getOrDefault("backoffMs", 800)).longValue(),
            ((Number) retryMap.getOrDefault("maxBackoffMs", 8000)).longValue()
        );

        Map<String, Object> rlMap = (Map<String, Object>) src.getOrDefault("rateLimit", Map.of());
        IngestConfig.RateLimit rateLimit = new IngestConfig.RateLimit(
            ((Number) rlMap.getOrDefault("qps", 2)).doubleValue(),
            (int) rlMap.getOrDefault("burst", 4)
        );

        int concurrency = ((Number) src.getOrDefault("concurrency", 4)).intValue();

        Map<String, Object> paMap = (Map<String, Object>) src.getOrDefault("paths", Map.of());
        IngestConfig.Paths paths = new IngestConfig.Paths(
            (String) paMap.getOrDefault("bronze", "data/bronze/" + sourceId),
            (String) paMap.getOrDefault("silver", "data/silver/" + sourceId),
            (String) paMap.getOrDefault("catalog", "catalog/glue.json")
        );

        log.info("Loaded config for source '{}' from {}", sourceId, path);
        return new IngestConfig(sourceId, baseUrl, symbols, concurrency, retry, rateLimit, paths);
    }

    @SuppressWarnings("unchecked")
    public static IngestConfig loadFromYaml(Path path, String sourceId, Map<String, Object> overrides) throws IOException {
        IngestConfig base = loadFromYaml(path, sourceId);
        return applyOverrides(base, overrides);
    }

    private static IngestConfig applyOverrides(IngestConfig base, Map<String, Object> overrides) {
        if (overrides == null || overrides.isEmpty()) return base;

        String baseUrl = overrides.containsKey("baseUrl") ? (String) overrides.get("baseUrl") : base.baseUrl();
        int concurrency = overrides.containsKey("concurrency") ? ((Number) overrides.get("concurrency")).intValue() : base.concurrency();

        return new IngestConfig(
            base.sourceId(),
            baseUrl,
            base.symbols(),
            concurrency,
            base.retry(),
            base.rateLimit(),
            base.paths()
        );
    }
}
