package com.hedgefund.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record HedgeConfig(
    String environment,
    int defaultConcurrency,
    DatalakeConfig datalake,
    ObservabilityConfig observability,
    Map<String, SourceEntry> sources
) {
    public record DatalakeConfig(String rootPath) {}
    public record ObservabilityConfig(LoggingConfig logging, MetricsConfig metrics, HealthConfig health) {}
    public record LoggingConfig(String level, String format) {}
    public record MetricsConfig(boolean enabled, String prefix) {}
    public record HealthConfig(boolean enabled, int port) {}
    public record SourceEntry(String baseUrl, List<String> symbols, int concurrency, Map<String, Object> extra) {}

    public static HedgeConfig defaults() {
        return new HedgeConfig(
            "development",
            4,
            new DatalakeConfig("datalake"),
            new ObservabilityConfig(
                new LoggingConfig("INFO", "text"),
                new MetricsConfig(true, "hedge"),
                new HealthConfig(true, 8081)
            ),
            Map.of()
        );
    }

    public static HedgeConfig load(Path path) throws IOException {
        if (!Files.exists(path)) {
            return defaults();
        }

        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(Files.newInputStream(path));
        return fromMap(root);
    }

    @SuppressWarnings("unchecked")
    public static HedgeConfig fromMap(Map<String, Object> root) {
        String env = (String) root.getOrDefault("environment", "development");
        int defaultConcurrency = ((Number) root.getOrDefault("defaultConcurrency", 4)).intValue();

        Map<String, Object> dlMap = (Map<String, Object>) root.getOrDefault("datalake", Map.of());
        DatalakeConfig datalake = new DatalakeConfig(
            (String) dlMap.getOrDefault("rootPath", "datalake")
        );

        Map<String, Object> obsMap = (Map<String, Object>) root.getOrDefault("observability", Map.of());
        Map<String, Object> logMap = (Map<String, Object>) obsMap.getOrDefault("logging", Map.of());
        Map<String, Object> metricsMap = (Map<String, Object>) obsMap.getOrDefault("metrics", Map.of());
        Map<String, Object> healthMap = (Map<String, Object>) obsMap.getOrDefault("health", Map.of());
        ObservabilityConfig observability = new ObservabilityConfig(
            new LoggingConfig(
                (String) logMap.getOrDefault("level", "INFO"),
                (String) logMap.getOrDefault("format", "text")
            ),
            new MetricsConfig(
                (Boolean) metricsMap.getOrDefault("enabled", true),
                (String) metricsMap.getOrDefault("prefix", "hedge")
            ),
            new HealthConfig(
                (Boolean) healthMap.getOrDefault("enabled", true),
                ((Number) healthMap.getOrDefault("port", 8081)).intValue()
            )
        );

        Map<String, Object> srcMap = (Map<String, Object>) root.getOrDefault("sources", Map.of());
        Map<String, SourceEntry> sources = new HashMap<>();
        srcMap.forEach((key, val) -> {
            Map<String, Object> s = (Map<String, Object>) val;
            sources.put(key, new SourceEntry(
                (String) s.getOrDefault("baseUrl", ""),
                (List<String>) s.getOrDefault("symbols", List.of()),
                ((Number) s.getOrDefault("concurrency", defaultConcurrency)).intValue(),
                s
            ));
        });

        return new HedgeConfig(env, defaultConcurrency, datalake, observability, sources);
    }

    public SourceEntry getSource(String sourceId) {
        return sources.get(sourceId);
    }

    public String resolveEnv(String key, String defaultValue) {
        String envKey = "HEDGE_" + key.toUpperCase().replace(".", "_");
        String envValue = System.getenv(envKey);
        return envValue != null ? envValue : defaultValue;
    }
}
