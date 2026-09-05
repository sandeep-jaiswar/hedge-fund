package com.hedgefund.worldbank.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedgefund.worldbank.config.WorldBankConfig;
import com.hedgefund.worldbank.model.DataPoint;
import com.hedgefund.worldbank.model.IndicatorMeta;
import com.hedgefund.worldbank.model.PageEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class WorldBankClient {
    private static final Logger log = LoggerFactory.getLogger(WorldBankClient.class);
    private final WorldBankConfig cfg;
    private final HttpClient http;
    private final ObjectMapper om;
    private final Semaphore rateSem;
    private long lastRequestAt = 0;

    public WorldBankClient(WorldBankConfig cfg) {
        this.cfg = cfg;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();
        this.om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.rateSem = new Semaphore(cfg.rateLimit().burst());
        // simple token refill via scheduled refill not needed; we do naive sleep for qps
    }

    private synchronized void throttle() throws InterruptedException {
        double qps = cfg.rateLimit().qps();
        long minGapMs = (long)(1000 / qps);
        long now = System.currentTimeMillis();
        long gap = now - lastRequestAt;
        if (gap < minGapMs) Thread.sleep(minGapMs - gap);
        lastRequestAt = System.currentTimeMillis();
    }

    /** List all indicator codes (for full crawl). Paginate /indicator?per_page=1000, filtered by source */
    public List<String> listAllIndicatorCodes() throws Exception {
        List<String> codes = new ArrayList<>();
        int page=1;
        int pages=1;
        String srcParam = cfg.source()!=null && !cfg.source().isBlank() ? "&source="+cfg.source() : "";
        do {
            String url = cfg.baseUrl()+"/indicator?format=json&per_page=1000&page="+page+srcParam;
            String body = fetchWithRetry(url);
            JsonNode root = om.readTree(body);
            if (!root.isArray() || root.size()<2) break;
            JsonNode meta = root.get(0);
            pages = meta.get("pages").asInt();
            JsonNode arr = root.get(1);
            for (JsonNode n: arr) {
                String id = n.get("id").asText();
                codes.add(id);
            }
            log.info("Listed indicators page {}/{} total {}", page, pages, codes.size());
            page++;
        } while (page<=pages);
        return codes;
    }

    public List<IndicatorMeta> fetchIndicatorMetas(List<String> codes) throws Exception {
        // fetch in batches but for meta, one by one is fine
        List<IndicatorMeta> out=new ArrayList<>();
        for(String code: codes){
            String url = cfg.baseUrl()+"/indicator/"+code+"?format=json";
            String body = fetchWithRetry(url);
            JsonNode root = om.readTree(body);
            JsonNode arr = root.get(1);
            if(arr!=null && arr.isArray() && arr.size()>0){
                IndicatorMeta meta = om.treeToValue(arr.get(0), IndicatorMeta.class);
                out.add(meta);
            }
        }
        return out;
    }

    public record FetchResult(PageEnvelope envelope, List<DataPoint> points, String rawBody){}

    /** Fetch one indicator batch (semicolon joined) for given countries and date */
    public FetchResult fetchPage(List<String> indicatorBatch, List<String> countries, String date, int page) throws Exception {
        String ind = String.join(";", indicatorBatch);
        String ctry = countries.size()==1 && countries.get(0).equals("all") ? "all" : String.join(";", countries);
        StringBuilder url = new StringBuilder(cfg.baseUrl()).append("/country/").append(ctry).append("/indicator/").append(ind)
                .append("?format=json&per_page=").append(cfg.perPage()).append("&page=").append(page);
        if (cfg.date()!=null && date!=null) url.append("&date=").append(date);
        else if (cfg.date()!=null) url.append("&date=").append(cfg.date());
        if (cfg.mrv()!=null) url.append("&mrv=").append(cfg.mrv());
        if (cfg.frequency()!=null) url.append("&frequency=").append(cfg.frequency());
        // source filter
        if (cfg.source()!=null && !cfg.source().isBlank()) url.append("&source=").append(cfg.source());
        String raw = fetchWithRetry(url.toString());
        JsonNode root = om.readTree(raw);
        if (!root.isArray() || root.size()<1) throw new IOException("Unexpected root: "+raw.substring(0, Math.min(200, raw.length())));
        PageEnvelope env = om.treeToValue(root.get(0), PageEnvelope.class);
        List<DataPoint> pts = new ArrayList<>();
        if (root.size()>=2 && root.get(1).isArray()){
            pts = om.convertValue(root.get(1), new TypeReference<List<DataPoint>>(){});
        }
        return new FetchResult(env, pts, raw);
    }

    /** Fetch all pages for a batch */
    public List<FetchResult> fetchAllPages(List<String> indicatorBatch, List<String> countries, String date) throws Exception {
        FetchResult first = fetchPage(indicatorBatch, countries, date, 1);
        List<FetchResult> all=new ArrayList<>();
        all.add(first);
        int pages = first.envelope().pages();
        if (cfg.maxPages()>0) pages = Math.min(pages, cfg.maxPages());
        if (pages>1){
            ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();
            List<Future<FetchResult>> futures = new ArrayList<>();
            Semaphore conc = new Semaphore(cfg.concurrency());
            for(int p=2;p<=pages;p++){
                int pp=p;
                conc.acquire();
                futures.add(exec.submit(()->{
                    try { return fetchPage(indicatorBatch, countries, date, pp); }
                    finally { conc.release(); }
                }));
            }
            for(Future<FetchResult> f: futures){ all.add(f.get()); }
            exec.shutdown();
        }
        return all;
    }

    private String fetchWithRetry(String url) throws Exception {
        int attempts=0;
        long backoff=cfg.retry().backoffMs();
        while(true){
            attempts++;
            try{
                throttle();
                HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().header("Accept","application/json").build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                if(resp.statusCode()==429 || resp.statusCode()>=500){
                    throw new IOException("HTTP "+resp.statusCode()+" "+resp.body().substring(0, Math.min(200, resp.body().length())));
                }
                if(resp.statusCode()!=200) throw new IOException("HTTP "+resp.statusCode()+" "+resp.body());
                return resp.body();
            } catch(Exception e){
                if(attempts>=cfg.retry().maxAttempts()) throw e;
                log.warn("Fetch failed attempt {}/{} {} -> retry in {}ms: {}", attempts, cfg.retry().maxAttempts(), url, backoff, e.toString());
                Thread.sleep(backoff + ThreadLocalRandom.current().nextInt(200));
                backoff = Math.min(backoff*2, cfg.retry().maxBackoffMs());
            }
        }
    }
}
