package com.hedgefund.observability.health;

import java.util.Map;

public record HealthStatus(Status status, Map<String, Object> details) {
    public enum Status { UP, DOWN, OUT_OF_SERVICE }

    public static HealthStatus up(Map<String, Object> details) {
        return new HealthStatus(Status.UP, details);
    }

    public static HealthStatus down(String reason) {
        return new HealthStatus(Status.DOWN, Map.of("error", reason));
    }

    public static HealthStatus outOfService(String reason) {
        return new HealthStatus(Status.OUT_OF_SERVICE, Map.of("reason", reason));
    }

    public boolean isUp() {
        return status == Status.UP;
    }
}
