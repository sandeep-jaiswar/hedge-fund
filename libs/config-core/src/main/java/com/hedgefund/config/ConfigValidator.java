package com.hedgefund.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class ConfigValidator {

    private static final Logger log = LoggerFactory.getLogger(ConfigValidator.class);

    private ConfigValidator() {}

    public static ValidationResult validate(HedgeConfig config) {
        ValidationResult result = new ValidationResult();

        if (config.environment() == null || config.environment().isBlank()) {
            result.addError("environment must not be blank");
        }

        if (config.defaultConcurrency() < 1 || config.defaultConcurrency() > 64) {
            result.addError("defaultConcurrency must be between 1 and 64, got: " + config.defaultConcurrency());
        }

        if (config.datalake() == null || config.datalake().rootPath() == null) {
            result.addError("datalake.rootPath must not be null");
        }

        if (config.observability() == null) {
            result.addError("observability config must not be null");
        }

        config.sources().forEach((sourceId, entry) -> {
            if (sourceId == null || sourceId.isBlank()) {
                result.addError("source ID must not be blank");
            }
            if (entry.concurrency() < 1 || entry.concurrency() > 32) {
                result.addError("source '" + sourceId + "' concurrency must be between 1 and 32");
            }
        });

        if (result.errors().isEmpty()) {
            log.info("Config validation passed for environment='{}', sources={}", config.environment(), config.sources().size());
        } else {
            log.error("Config validation failed with {} errors: {}", result.errors().size(), result.errors());
        }

        return result;
    }

    public static ValidationResult validateAndFailFast(HedgeConfig config) {
        ValidationResult result = validate(config);
        if (!result.isValid()) {
            throw new IllegalStateException("Config validation failed: " + result.errors());
        }
        return result;
    }

    public record ValidationResult(java.util.List<String> errors) {
        public ValidationResult() {
            this(new java.util.ArrayList<>());
        }
        public void addError(String error) {
            errors.add(error);
        }
        public boolean isValid() {
            return errors.isEmpty();
        }
    }
}
