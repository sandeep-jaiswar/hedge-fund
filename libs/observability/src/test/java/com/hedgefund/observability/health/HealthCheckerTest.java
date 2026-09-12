package com.hedgefund.observability.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HealthCheckerTest {

    @Test
    void checkAllReturnsUpWhenHealthy(@TempDir Path tempDir) throws IOException {
        // Create minimal datalake structure
        Files.createDirectories(tempDir.resolve("data/bronze"));
        Files.createDirectories(tempDir.resolve("data/silver"));
        Files.createDirectories(tempDir.resolve("catalog"));
        Files.writeString(tempDir.resolve("catalog/glue.json"), "{}");

        HealthChecker checker = new HealthChecker(tempDir);
        Map<String, Object> result = checker.checkAll();

        assertEquals("UP", result.get("status"));
        assertNotNull(result.get("components"));
        assertNotNull(result.get("timestamp"));
    }

    @Test
    void checkAllReturnsDownWhenMissing(@TempDir Path tempDir) {
        // Empty temp dir, no datalake structure
        HealthChecker checker = new HealthChecker(tempDir);
        Map<String, Object> result = checker.checkAll();

        assertEquals("DOWN", result.get("status"));
    }

    @Test
    void customIndicatorIsIncluded(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("data/bronze"));
        Files.createDirectories(tempDir.resolve("data/silver"));
        Files.createDirectories(tempDir.resolve("catalog"));
        Files.writeString(tempDir.resolve("catalog/glue.json"), "{}");

        HealthChecker checker = new HealthChecker(tempDir);
        checker.register(new HealthIndicator() {
            @Override public String name() { return "custom"; }
            @Override public HealthStatus check() {
                return HealthStatus.up(Map.of("custom", true));
            }
        });

        Map<String, Object> result = checker.checkAll();
        assertEquals("UP", result.get("status"));
        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) result.get("components");
        assertNotNull(components.get("custom"));
    }

    @Test
    void checkAsJsonReturnsValidJson(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("data/bronze"));
        Files.createDirectories(tempDir.resolve("data/silver"));
        Files.createDirectories(tempDir.resolve("catalog"));
        Files.writeString(tempDir.resolve("catalog/glue.json"), "{}");

        HealthChecker checker = new HealthChecker(tempDir);
        String json = checker.checkAsJson();

        assertNotNull(json);
        assertTrue(json.contains("UP"));
        assertTrue(json.contains("datalake"));
    }

    @Test
    void diskSpaceIndicatorReportsFreeSpace(@TempDir Path tempDir) {
        HealthChecker checker = new HealthChecker(tempDir);
        Map<String, Object> result = checker.checkAll();

        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) result.get("components");
        Map<String, Object> disk = (Map<String, Object>) components.get("diskSpace");
        assertNotNull(disk);
        assertEquals("UP", disk.get("status"));
    }
}
