package com.hedgefund.observability.logging;

import org.slf4j.MDC;

import java.util.UUID;

public final class CorrelationId {

    private static final String CORRELATION_KEY = "correlationId";
    private static final String SOURCE_KEY = "source";
    private static final String JOB_KEY = "jobId";

    private CorrelationId() {}

    public static String generate() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static void set(String id) {
        MDC.put(CORRELATION_KEY, id);
    }

    public static String get() {
        String id = MDC.get(CORRELATION_KEY);
        return id != null ? id : "unknown";
    }

    public static void setSource(String source) {
        MDC.put(SOURCE_KEY, source);
    }

    public static String getSource() {
        String src = MDC.get(SOURCE_KEY);
        return src != null ? src : "unknown";
    }

    public static void setJobId(String jobId) {
        MDC.put(JOB_KEY, jobId);
    }

    public static String getJobId() {
        String id = MDC.get(JOB_KEY);
        return id != null ? id : "none";
    }

    public static void clear() {
        MDC.remove(CORRELATION_KEY);
        MDC.remove(SOURCE_KEY);
        MDC.remove(JOB_KEY);
    }

    public static String withContext(String source) {
        String id = generate();
        set(id);
        setSource(source);
        return id;
    }
}
