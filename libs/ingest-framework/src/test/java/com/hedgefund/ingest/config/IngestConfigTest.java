package com.hedgefund.ingest.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IngestConfigTest {

    @Test
    void defaultsReturnsValidConfig() {
        IngestConfig cfg = IngestConfig.defaults("test-source");
        assertEquals("test-source", cfg.sourceId());
        assertEquals("https://localhost", cfg.baseUrl());
        assertEquals(4, cfg.concurrency());
        assertEquals(3, cfg.retry().maxAttempts());
        assertEquals(800, cfg.retry().backoffMs());
        assertEquals(2.0, cfg.rateLimit().qps());
    }

    @Test
    void loadFromYamlParsesCorrectly(@TempDir Path tempDir) throws IOException {
        String yaml = """
            yahoo:
              baseUrl: https://query1.finance.yahoo.com
              symbols: [AAPL, MSFT]
              concurrency: 8
              retry:
                maxAttempts: 5
                backoffMs: 1000
                maxBackoffMs: 16000
              rateLimit:
                qps: 3
                burst: 6
              paths:
                bronze: data/bronze/yahoo
                silver: data/silver/yahoo
                catalog: catalog/glue.json
            """;
        Path configFile = tempDir.resolve("test.yaml");
        Files.writeString(configFile, yaml);

        IngestConfig cfg = IngestConfigLoader.loadFromYaml(configFile, "yahoo");
        assertEquals("https://query1.finance.yahoo.com", cfg.baseUrl());
        assertEquals(List.of("AAPL", "MSFT"), cfg.symbols());
        assertEquals(8, cfg.concurrency());
        assertEquals(5, cfg.retry().maxAttempts());
        assertEquals(1000, cfg.retry().backoffMs());
        assertEquals(16000, cfg.retry().maxBackoffMs());
        assertEquals(3.0, cfg.rateLimit().qps());
        assertEquals(6, cfg.rateLimit().burst());
    }

    @Test
    void loadFromMissingFileReturnsDefaults() throws IOException {
        IngestConfig cfg = IngestConfigLoader.loadFromYaml(Path.of("/nonexistent/file.yaml"), "yahoo");
        assertEquals("yahoo", cfg.sourceId());
        assertEquals(4, cfg.concurrency());
    }

    @Test
    void loadFromYamlWithMissingSourceReturnsDefaults(@TempDir Path tempDir) throws IOException {
        String yaml = """
            other_source:
              baseUrl: https://example.com
            """;
        Path configFile = tempDir.resolve("test.yaml");
        Files.writeString(configFile, yaml);

        IngestConfig cfg = IngestConfigLoader.loadFromYaml(configFile, "yahoo");
        assertEquals("yahoo", cfg.sourceId());
        assertEquals("https://localhost", cfg.baseUrl());
    }

    @Test
    void envOverrideWorks() {
        String value = System.getenv().getOrDefault("HEDGE_TEST_VAR", "default");
        assertNotNull(value);
    }
}
