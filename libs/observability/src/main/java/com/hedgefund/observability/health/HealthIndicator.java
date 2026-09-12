package com.hedgefund.observability.health;

public interface HealthIndicator {
    String name();
    HealthStatus check();
}
