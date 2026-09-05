package com.hedgefund.investing.config;
import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public record InvestingConfig(String baseUrl, List<String> symbols, List<String> series, List<String> tickers, List<String> protocols, String interval, int limit, int concurrency, Retry retry, RateLimit rateLimit, Paths paths) {
    public record Retry(int maxAttempts, long backoffMs, long maxBackoffMs){}
    public record RateLimit(double qps, int burst){}
    public record Paths(String bronze, String silver, String catalog){}
    @SuppressWarnings("unchecked")
    public static InvestingConfig fromYaml(Path p) throws IOException {
        Yaml yaml=new Yaml();
        Map<String,Object> root=yaml.load(Files.newInputStream(p));
        Map<String,Object> m=(Map<String,Object>)root.get("investing");
        if(m==null) m=new HashMap<>();
        String baseUrl=(String)m.getOrDefault("baseUrl","https://api.investing.com");
        List<String> symbols=(List<String>)m.getOrDefault("symbols", m.getOrDefault("series", m.getOrDefault("tickers", m.getOrDefault("protocols", List.of("investing")))));
        // normalize
        List<String> series = (List<String>)m.getOrDefault("series", symbols);
        List<String> tickers = (List<String>)m.getOrDefault("tickers", symbols);
        List<String> protocols = (List<String>)m.getOrDefault("protocols", symbols);
        String interval=(String)m.getOrDefault("interval","1d");
        int limit=((Number)m.getOrDefault("limit",30)).intValue();
        Map<String,Object> rc=(Map<String,Object>)m.getOrDefault("retry", Map.of());
        Retry retry=new Retry((int)rc.getOrDefault("maxAttempts",3), ((Number)rc.getOrDefault("backoffMs",800)).longValue(), ((Number)rc.getOrDefault("maxBackoffMs",8000)).longValue());
        Map<String,Object> rl=(Map<String,Object>)m.getOrDefault("rateLimit", Map.of());
        RateLimit rateLimit=new RateLimit(((Number)rl.getOrDefault("qps",2)).doubleValue(), (int)rl.getOrDefault("burst",4));
        int concurrency=((Number)m.getOrDefault("concurrency",2)).intValue();
        Map<String,Object> pa=(Map<String,Object>)m.getOrDefault("paths", Map.of());
        Paths paths=new Paths((String)pa.getOrDefault("bronze","data/bronze/investing"), (String)pa.getOrDefault("silver","data/silver/investing"), (String)pa.getOrDefault("catalog","catalog/glue.json"));
        return new InvestingConfig(baseUrl, symbols, series, tickers, protocols, interval, limit, concurrency, retry, rateLimit, paths);
    }
    public static InvestingConfig defaults(){ return new InvestingConfig("https://api.investing.com", List.of("investing"), List.of("investing"), List.of("investing"), List.of("investing"), "1d",30,2,new Retry(3,800,8000), new RateLimit(2,4), new Paths("data/bronze/investing","data/silver/investing","catalog/glue.json")); }
    // convenience
    public List<String> effectiveKeys(){
        if(!series.isEmpty() && !series.get(0).equals("investing")) return series;
        if(!tickers.isEmpty() && !tickers.get(0).equals("investing")) return tickers;
        if(!protocols.isEmpty() && !protocols.get(0).equals("investing")) return protocols;
        return symbols;
    }
}
