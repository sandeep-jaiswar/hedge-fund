package com.hedgefund.eastmoney.client;
import com.fasterxml.jackson.databind.*;
import com.hedgefund.eastmoney.config.EastmoneyConfig;
import org.slf4j.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.*;
public class EastmoneyClient {
    private static final Logger log=LoggerFactory.getLogger(EastmoneyClient.class);
    private final EastmoneyConfig cfg;
    private final HttpClient http;
    private final ObjectMapper om;
    private long lastRequestAt=0;
    public EastmoneyClient(EastmoneyConfig cfg){
        this.cfg=cfg;
        this.http=HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(15)).executor(Executors.newVirtualThreadPerTaskExecutor()).build();
        this.om=new ObjectMapper(); om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
    }
    private synchronized void throttle() throws InterruptedException {
        double qps=cfg.rateLimit().qps();
        long minGapMs=(long)(1000/qps);
        long now=System.currentTimeMillis();
        long gap=now-lastRequestAt;
        if(gap<minGapMs) Thread.sleep(minGapMs-gap);
        lastRequestAt=System.currentTimeMillis();
    }
    public String fetchRaw(String url) throws Exception {
        int attempts=0; long backoff=cfg.retry().backoffMs();
        while(true){
            attempts++;
            try{
                throttle();
                HttpRequest.Builder b=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).GET()
                    .header("User-Agent","Mozilla/5.0 HedgeFund/1.0").header("Accept","*/*");
                // SEC requires Host header and extra UA
                if(url.contains("sec.gov")) b.header("Accept-Encoding","gzip");
                HttpResponse<String> resp=http.send(b.build(), HttpResponse.BodyHandlers.ofString());
                if(resp.statusCode()==429 || resp.statusCode()>=500) throw new java.io.IOException("HTTP "+resp.statusCode()+" "+resp.body().substring(0,Math.min(300,resp.body().length())));
                if(resp.statusCode()!=200) throw new java.io.IOException("HTTP "+resp.statusCode()+" "+resp.body().substring(0,Math.min(400,resp.body().length())));
                return resp.body();
            }catch(Exception e){
                if(attempts>=cfg.retry().maxAttempts()) throw e;
                log.warn("Fetch failed {}/{} {} -> retry {}ms {}",attempts,cfg.retry().maxAttempts(),url,backoff,e.toString());
                Thread.sleep(backoff+ThreadLocalRandom.current().nextInt(300));
                backoff=Math.min(backoff*2,cfg.retry().maxBackoffMs());
            }
        }
    }
    // typed fetchers per source - keep generic json fetch for now
    public JsonNode fetchJson(String url) throws Exception {
        String raw=fetchRaw(url);
        return om.readTree(raw);
    }
}
