package com.hedgefund.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigValidatorTest {

    @Test
    void validConfigPassesValidation() {
        HedgeConfig cfg = HedgeConfig.defaults();
        ConfigValidator.ValidationResult result = ConfigValidator.validate(cfg);
        assertTrue(result.isValid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void invalidConcurrencyFailsValidation() {
        HedgeConfig cfg = new HedgeConfig(
            "test", -1,
            new HedgeConfig.DatalakeConfig("/tmp"),
            new HedgeConfig.ObservabilityConfig(
                new HedgeConfig.LoggingConfig("INFO", "text"),
                new HedgeConfig.MetricsConfig(true, "hedge"),
                new HedgeConfig.HealthConfig(true, 8081)
            ),
            java.util.Map.of()
        );
        ConfigValidator.ValidationResult result = ConfigValidator.validate(cfg);
        assertFalse(result.isValid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("defaultConcurrency")));
    }

    @Test
    void validateAndFailFastThrowsOnInvalid() {
        HedgeConfig cfg = new HedgeConfig(
            "", 100,
            new HedgeConfig.DatalakeConfig("/tmp"),
            new HedgeConfig.ObservabilityConfig(
                new HedgeConfig.LoggingConfig("INFO", "text"),
                new HedgeConfig.MetricsConfig(true, "hedge"),
                new HedgeConfig.HealthConfig(true, 8081)
            ),
            java.util.Map.of()
        );

        assertThrows(IllegalStateException.class, () -> ConfigValidator.validateAndFailFast(cfg));
    }

    @Test
    void sourceConcurrencyValidation() {
        java.util.Map<String, HedgeConfig.SourceEntry> sources = java.util.Map.of(
            "bad", new HedgeConfig.SourceEntry("https://x", java.util.List.of(), 100, java.util.Map.of())
        );
        HedgeConfig cfg = new HedgeConfig(
            "test", 4,
            new HedgeConfig.DatalakeConfig("/tmp"),
            new HedgeConfig.ObservabilityConfig(
                new HedgeConfig.LoggingConfig("INFO", "text"),
                new HedgeConfig.MetricsConfig(true, "hedge"),
                new HedgeConfig.HealthConfig(true, 8081)
            ),
            sources
        );
        ConfigValidator.ValidationResult result = ConfigValidator.validate(cfg);
        assertFalse(result.isValid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("'bad' concurrency")));
    }
}
