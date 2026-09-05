package com.hedgefund.yahoo.config;
import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public record YahooConfig(String baseUrl, List<String> symbols, String interval, String range, int concurrency, Retry retry, RateLimit rateLimit, Paths paths) {
    public record Retry(int maxAttempts, long backoffMs, long maxBackoffMs){}
    public record RateLimit(double qps, int burst){}
    public record Paths(String bronze, String silver, String catalog){}
    public static YahooConfig fromYaml(Path p) throws IOException {
        Yaml yaml=new Yaml();
        Map<String,Object> root=yaml.load(Files.newInputStream(p));
        Map<String,Object> y=(Map<String,Object>)root.get("yahoo");
        String baseUrl=(String)y.getOrDefault("baseUrl","https://query1.finance.yahoo.com");
        List<String> symbols=(List<String>)y.getOrDefault("symbols", List.of("AAPL"));
        String interval=(String)y.getOrDefault("interval","1d");
        String range=(String)y.getOrDefault("range","1mo");
        Map<String,Object> rc=(Map<String,Object>)y.getOrDefault("retry", Map.of());
        Retry retry=new Retry((int)rc.getOrDefault("maxAttempts",3), ((Number)rc.getOrDefault("backoffMs",800)).longValue(), ((Number)rc.getOrDefault("maxBackoffMs",8000)).longValue());
        Map<String,Object> rl=(Map<String,Object>)y.getOrDefault("rateLimit", Map.of());
        RateLimit rateLimit=new RateLimit(((Number)rl.getOrDefault("qps",2)).doubleValue(), (int)rl.getOrDefault("burst",4));
        int concurrency=((Number)y.getOrDefault("concurrency",4)).intValue();
        Map<String,Object> pa=(Map<String,Object>)y.getOrDefault("paths", Map.of());
        Paths paths=new Paths((String)pa.getOrDefault("bronze","data/bronze/yahoo"), (String)pa.getOrDefault("silver","data/silver/yahoo"), (String)pa.getOrDefault("catalog","catalog/glue.json"));
        return new YahooConfig(baseUrl,symbols,interval,range,concurrency,retry,rateLimit,paths);
    }
    public static YahooConfig defaults(){ return new YahooConfig("https://query1.finance.yahoo.com", List.of("AAPL"), "1d","1mo",4,new Retry(3,800,8000), new RateLimit(2,4), new Paths("data/bronze/yahoo","data/silver/yahoo","catalog/glue.json")); }
}
