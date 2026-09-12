package com.hedgefund.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HedgeConfigTest {

    @Test
    void defaultsReturnsValidConfig() {
        HedgeConfig cfg = HedgeConfig.defaults();
        assertEquals("development", cfg.environment());
        assertEquals(4, cfg.defaultConcurrency());
        assertNotNull(cfg.datalake());
        assertNotNull(cfg.observability());
        assertNotNull(cfg.sources());
        assertTrue(cfg.sources().isEmpty());
    }

    @Test
    void loadFromMissingFileReturnsDefaults() throws IOException {
        HedgeConfig cfg = HedgeConfig.load(Path.of("/nonexistent/config.yaml"));
        assertEquals("development", cfg.environment());
    }

    @Test
    void loadFromYamlParsesCorrectly(@TempDir Path tempDir) throws IOException {
        String yaml = """
            environment: production
            defaultConcurrency: 8
            datalake:
              rootPath: /data/datalake
            observability:
              logging:
                level: WARN
                format: json
              metrics:
                enabled: true
                prefix: hedge
              health:
                enabled: true
                port: 9090
            sources:
              yahoo:
                baseUrl: https://query1.finance.yahoo.com
                symbols: [AAPL, MSFT]
                concurrency: 4
              worldbank:
                baseUrl: https://api.worldbank.org
                indicators: [NY.GDP.MKTP.CD]
                concurrency: 2
            """;
        Path configFile = tempDir.resolve("hedge.yaml");
        Files.writeString(configFile, yaml);

        HedgeConfig cfg = HedgeConfig.load(configFile);
        assertEquals("production", cfg.environment());
        assertEquals(8, cfg.defaultConcurrency());
        assertEquals("/data/datalake", cfg.datalake().rootPath());
        assertEquals("WARN", cfg.observability().logging().level());
        assertEquals("json", cfg.observability().logging().format());
        assertEquals(true, cfg.observability().metrics().enabled());
        assertEquals("hedge", cfg.observability().metrics().prefix());
        assertEquals(9090, cfg.observability().health().port());
        assertEquals(2, cfg.sources().size());
    }

    @Test
    void getSourceReturnsEntry(@TempDir Path tempDir) throws IOException {
        String yaml = """
            sources:
              yahoo:
                baseUrl: https://example.com
                symbols: [AAPL]
                concurrency: 2
            """;
        Path configFile = tempDir.resolve("hedge.yaml");
        Files.writeString(configFile, yaml);

        HedgeConfig cfg = HedgeConfig.load(configFile);
        HedgeConfig.SourceEntry yahoo = cfg.getSource("yahoo");
        assertNotNull(yahoo);
        assertEquals("https://example.com", yahoo.baseUrl());
    }

    @Test
    void getSourceReturnsNullForMissing(@TempDir Path tempDir) throws IOException {
        String yaml = "sources: {}";
        Path configFile = tempDir.resolve("hedge.yaml");
        Files.writeString(configFile, yaml);

        HedgeConfig cfg = HedgeConfig.load(configFile);
        assertNull(cfg.getSource("nonexistent"));
    }

    @Test
    void fromMapParsesCorrectly() {
        Map<String, Object> map = Map.of(
            "environment", "test",
            "defaultConcurrency", 2,
            "datalake", Map.of("rootPath", "/tmp/datalake"),
            "observability", Map.of(
                "logging", Map.of("level", "DEBUG", "format", "text"),
                "metrics", Map.of("enabled", false, "prefix", "test"),
                "health", Map.of("enabled", true, "port", 8081)
            ),
            "sources", Map.of()
        );

        HedgeConfig cfg = HedgeConfig.fromMap(map);
        assertEquals("test", cfg.environment());
        assertEquals(2, cfg.defaultConcurrency());
        assertEquals("/tmp/datalake", cfg.datalake().rootPath());
        assertEquals("DEBUG", cfg.observability().logging().level());
        assertEquals(false, cfg.observability().metrics().enabled());
    }
}
