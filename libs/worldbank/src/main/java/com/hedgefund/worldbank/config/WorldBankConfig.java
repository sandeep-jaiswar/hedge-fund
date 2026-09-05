package com.hedgefund.worldbank.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Configurable feed definition.
 * YAML at config/worldbank/worldbank.yaml, env overrides WORLD_BANK_*.
 */
public record WorldBankConfig(
        String baseUrl,
        List<String> indicators,
        List<String> countries,
        String date,
        Integer mrv,
        String frequency,
        int perPage,
        int concurrency,
        int maxIndicatorsPerRequest,
        int maxCountriesPerRequest,
        Retry retry,
        RateLimit rateLimit,
        Paths paths,
        boolean fullCrawl,
        int maxPages,
        String source
) {
    public record Retry(int maxAttempts, long backoffMs, long maxBackoffMs) {}
    public record RateLimit(double qps, int burst) {}
    public record Paths(String bronze, String silver, String catalog) {}

    @SuppressWarnings("unchecked")
    public static WorldBankConfig fromYaml(Path p) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(p)) {
            root = yaml.load(in);
        }
        Map<String, Object> wb = (Map<String, Object>) root.getOrDefault("worldbank", root);
        String baseUrl = str(wb, "baseUrl", "https://api.worldbank.org/v2");
        boolean fullCrawl = bool(wb, "fullCrawl", false);
        List<String> indicators = list(wb, "indicators");
        List<String> countries = list(wb, "countries");
        if (countries.isEmpty()) countries = List.of("all");
        String date = str(wb, "date", "2010:2024");
        Integer mrv = wb.containsKey("mrv") && wb.get("mrv") != null ? Integer.valueOf(wb.get("mrv").toString()) : null;
        String frequency = str(wb, "frequency", null);
        int perPage = intVal(wb, "perPage", 1000);
        int concurrency = intVal(wb, "concurrency", 8);
        int maxInd = intVal(wb, "maxIndicatorsPerRequest", 20);
        int maxCtry = intVal(wb, "maxCountriesPerRequest", 20);
        int maxPages = intVal(wb, "maxPages", 0);
        String source = str(wb, "source", "2");

        Map<String, Object> retryM = (Map<String, Object>) wb.getOrDefault("retry", Map.of());
        Retry retry = new Retry(intVal(retryM, "maxAttempts", 3), longVal(retryM, "backoffMs", 500), longVal(retryM, "maxBackoffMs", 8000));
        Map<String, Object> rlM = (Map<String, Object>) wb.getOrDefault("rateLimit", Map.of());
        RateLimit rl = new RateLimit(doubleVal(rlM, "qps", 5), intVal(rlM, "burst", 10));
        Map<String, Object> pathsM = (Map<String, Object>) wb.getOrDefault("paths", Map.of());
        Paths paths = new Paths(str(pathsM, "bronze", "data/bronze/worldbank"), str(pathsM, "silver", "data/silver/worldbank"), str(pathsM, "catalog", "catalog/glue.json"));

        // env overrides
        String envBase = System.getenv("WORLDBANK_BASE_URL");
        if (envBase != null) baseUrl = envBase;

        return new WorldBankConfig(baseUrl, indicators, countries, date, mrv, frequency, perPage, concurrency, maxInd, maxCtry, retry, rl, paths, fullCrawl, maxPages, source);
    }

    public static WorldBankConfig defaults() {
        return new WorldBankConfig("https://api.worldbank.org/v2", List.of(), List.of("all"), "2010:2024", null, null, 1000, 8, 20, 20, new Retry(3,500,8000), new RateLimit(5,10), new Paths("data/bronze/worldbank","data/silver/worldbank","catalog/glue.json"), false, 0, "2");
    }

    private static String str(Map<String,Object> m, String k, String def){ Object v=m.get(k); return v==null?def:v.toString();}
    private static boolean bool(Map<String,Object> m,String k,boolean def){ Object v=m.get(k); return v==null?def: Boolean.parseBoolean(v.toString());}
    private static int intVal(Map<String,Object> m,String k,int def){ Object v=m.get(k); return v==null?def: Integer.parseInt(v.toString());}
    private static long longVal(Map<String,Object> m,String k,long def){ Object v=m.get(k); return v==null?def: Long.parseLong(v.toString());}
    private static double doubleVal(Map<String,Object> m,String k,double def){ Object v=m.get(k); return v==null?def: Double.parseDouble(v.toString());}
    @SuppressWarnings("unchecked")
    private static List<String> list(Map<String,Object> m,String k){ Object v=m.get(k); if(v==null) return List.of(); if(v instanceof List) return ((List<Object>)v).stream().map(Object::toString).toList(); return List.of(v.toString());}
}
