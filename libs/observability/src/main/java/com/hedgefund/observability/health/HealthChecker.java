package com.hedgefund.observability.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HealthChecker {

    private static final Logger log = LoggerFactory.getLogger(HealthChecker.class);
    private static final ObjectMapper om = new ObjectMapper();

    private final List<HealthIndicator> indicators = new ArrayList<>();
    private final Path datalakeRoot;

    public HealthChecker(Path datalakeRoot) {
        this.datalakeRoot = datalakeRoot;
        registerDefaultIndicators();
    }

    public void register(HealthIndicator indicator) {
        indicators.add(indicator);
    }

    public Map<String, Object> checkAll() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", Instant.now().toString());

        String overallStatus = "UP";
        Map<String, Object> components = new LinkedHashMap<>();

        for (HealthIndicator indicator : indicators) {
            try {
                HealthStatus status = indicator.check();
                components.put(indicator.name(), Map.of(
                    "status", status.status().name(),
                    "details", status.details()
                ));
                if (!status.isUp()) {
                    overallStatus = "DOWN";
                }
            } catch (Exception e) {
                components.put(indicator.name(), Map.of(
                    "status", "DOWN",
                    "details", Map.of("error", e.getMessage())
                ));
                overallStatus = "DOWN";
            }
        }

        result.put("status", overallStatus);
        result.put("components", components);
        return result;
    }

    public String checkAsJson() {
        try {
            return om.writerWithDefaultPrettyPrinter().writeValueAsString(checkAll());
        } catch (Exception e) {
            return "{\"status\":\"DOWN\",\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    private void registerDefaultIndicators() {
        register(new DatalakeHealthIndicator(datalakeRoot));
        register(new DiskSpaceHealthIndicator(datalakeRoot));
    }

    private static class DatalakeHealthIndicator implements HealthIndicator {
        private final Path root;

        DatalakeHealthIndicator(Path root) {
            this.root = root;
        }

        @Override
        public String name() { return "datalake"; }

        @Override
        public HealthStatus check() {
            Path catalog = root.resolve("catalog/glue.json");
            Path bronze = root.resolve("data/bronze");
            Path silver = root.resolve("data/silver");
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("catalogExists", Files.exists(catalog));
            details.put("bronzeExists", Files.exists(bronze));
            details.put("silverExists", Files.exists(silver));
            details.put("rootPath", root.toString());

            if (Files.exists(bronze)) {
                try {
                    long count = Files.list(bronze).count();
                    details.put("bronzeSources", count);
                } catch (Exception e) {
                    details.put("bronzeError", e.getMessage());
                }
            }

            boolean healthy = Files.exists(bronze) && Files.exists(silver);
            return healthy ? HealthStatus.up(details) : HealthStatus.down("Datalake directories missing");
        }
    }

    private static class DiskSpaceHealthIndicator implements HealthIndicator {
        private final Path root;

        DiskSpaceHealthIndicator(Path root) {
            this.root = root;
        }

        @Override
        public String name() { return "diskSpace"; }

        @Override
        public HealthStatus check() {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("totalSpaceGB", root.toFile().getTotalSpace() / (1024 * 1024 * 1024));
            details.put("freeSpaceGB", root.toFile().getFreeSpace() / (1024 * 1024 * 1024));
            long usedPercent = 100 - (root.toFile().getFreeSpace() * 100 / root.toFile().getTotalSpace());
            details.put("usedPercent", usedPercent);

            if (usedPercent > 90) {
                return HealthStatus.outOfService("Disk usage critical: " + usedPercent + "%");
            }
            return HealthStatus.up(details);
        }
    }
}
