package com.hedgefund.defillama.config;
import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.nio.file.*;
import java.util.*;
public record DefillamaConfig(String baseUrl, List<String> symbols, List<String> series, List<String> tickers, List<String> protocols, String interval, int limit, int concurrency, Retry retry, RateLimit rateLimit, Paths paths) {
    public record Retry(int maxAttempts, long backoffMs, long maxBackoffMs){}
    public record RateLimit(double qps, int burst){}
    public record Paths(String bronze, String silver, String catalog){}
    @SuppressWarnings("unchecked")
    public static DefillamaConfig fromYaml(Path p) throws IOException {
        Yaml yaml=new Yaml();
        Map<String,Object> root=yaml.load(Files.newInputStream(p));
        Map<String,Object> m=(Map<String,Object>)root.get("defillama");
        if(m==null) m=new HashMap<>();
        String baseUrl=(String)m.getOrDefault("baseUrl","https://api.llama.fi");
        List<String> symbols=(List<String>)m.getOrDefault("symbols", m.getOrDefault("series", m.getOrDefault("tickers", m.getOrDefault("protocols", List.of("defillama")))));
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
        Paths paths=new Paths((String)pa.getOrDefault("bronze","data/bronze/defillama"), (String)pa.getOrDefault("silver","data/silver/defillama"), (String)pa.getOrDefault("catalog","catalog/glue.json"));
        return new DefillamaConfig(baseUrl, symbols, series, tickers, protocols, interval, limit, concurrency, retry, rateLimit, paths);
    }
    public static DefillamaConfig defaults(){ return new DefillamaConfig("https://api.llama.fi", List.of("defillama"), List.of("defillama"), List.of("defillama"), List.of("defillama"), "1d",30,2,new Retry(3,800,8000), new RateLimit(2,4), new Paths("data/bronze/defillama","data/silver/defillama","catalog/glue.json")); }
    // convenience
    public List<String> effectiveKeys(){
        if(!series.isEmpty() && !series.get(0).equals("defillama")) return series;
        if(!tickers.isEmpty() && !tickers.get(0).equals("defillama")) return tickers;
        if(!protocols.isEmpty() && !protocols.get(0).equals("defillama")) return protocols;
        return symbols;
    }
}
