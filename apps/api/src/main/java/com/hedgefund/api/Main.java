package com.hedgefund.api;

import com.hedgefund.common.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Starting hedge-fund API...");

        var balance = Money.of(1_000_000, "USD");
        System.out.println("Balance: " + balance);

        // Java 21: virtual threads friendly - no heavy framework needed for SIMPLE start
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> log.info("API ready on :8080 (placeholder)"));
        }

        log.info("API started. Balance={}", balance);
    }
}
