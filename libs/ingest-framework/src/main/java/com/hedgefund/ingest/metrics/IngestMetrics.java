package com.hedgefund.ingest.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class IngestMetrics {

    private static final Logger log = LoggerFactory.getLogger(IngestMetrics.class);

    private final Map<String, LongAdder> counters = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> errorCounters = new ConcurrentHashMap<>();

    public void incrementCounter(String name, String... tags) {
        String key = buildKey(name, tags);
        counters.computeIfAbsent(key, k -> new LongAdder()).increment();
    }

    public void incrementError(String name, String... tags) {
        String key = buildKey(name, tags);
        errorCounters.computeIfAbsent(key, k -> new LongAdder()).increment();
    }

    public long getCounter(String name, String... tags) {
        String key = buildKey(name, tags);
        LongAdder adder = counters.get(key);
        return adder != null ? adder.sum() : 0;
    }

    public long getErrorCount(String name, String... tags) {
        String key = buildKey(name, tags);
        LongAdder adder = errorCounters.get(key);
        return adder != null ? adder.sum() : 0;
    }

    public Map<String, Long> snapshot() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        counters.forEach((k, v) -> result.put(k, v.sum()));
        errorCounters.forEach((k, v) -> result.put("error." + k, v.sum()));
        return result;
    }

    public void logSummary() {
        counters.forEach((k, v) -> log.info("METRIC {} = {}", k, v.sum()));
        errorCounters.forEach((k, v) -> log.info("METRIC error.{} = {}", k, v.sum()));
    }

    private String buildKey(String name, String[] tags) {
        if (tags.length == 0) return name;
        StringBuilder sb = new StringBuilder(name);
        for (int i = 0; i < tags.length; i += 2) {
            if (i + 1 < tags.length) {
                sb.append(".").append(tags[i]).append("=").append(tags[i + 1]);
            }
        }
        return sb.toString();
    }
}
