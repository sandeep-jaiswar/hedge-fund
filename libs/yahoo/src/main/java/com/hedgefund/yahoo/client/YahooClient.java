package com.hedgefund.yahoo.client;
import com.fasterxml.jackson.databind.*;
import com.hedgefund.yahoo.config.YahooConfig;
import com.hedgefund.yahoo.model.Bar;
import org.slf4j.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
public class YahooClient {
    private static final Logger log=LoggerFactory.getLogger(YahooClient.class);
    private final YahooConfig cfg;
    private final HttpClient http;
    private final ObjectMapper om;
    private long lastRequestAt=0;
    public YahooClient(YahooConfig cfg){
        this.cfg=cfg;
        this.http=HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).executor(Executors.newVirtualThreadPerTaskExecutor()).build();
        this.om=new ObjectMapper(); om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
    }
    private synchronized void throttle() throws InterruptedException{
        double qps=cfg.rateLimit().qps();
        long minGapMs=(long)(1000/qps);
        long now=System.currentTimeMillis();
        long gap=now-lastRequestAt;
        if(gap<minGapMs) Thread.sleep(minGapMs-gap);
        lastRequestAt=System.currentTimeMillis();
    }
    public List<Bar> fetchChart(String symbol) throws Exception {
        String url=cfg.baseUrl()+"/v8/finance/chart/"+URLEncoder.encode(symbol, java.nio.charset.StandardCharsets.UTF_8)+"?interval="+cfg.interval()+"&range="+cfg.range()+"&includePrePost=false";
        String body=fetchWithRetry(url);
        JsonNode root=om.readTree(body);
        JsonNode result=root.path("chart").path("result");
        if(!result.isArray()||result.size()==0) throw new java.io.IOException("No result for "+symbol+": "+body.substring(0,Math.min(400,body.length())));
        JsonNode r=result.get(0);
        JsonNode timestamps=r.path("timestamp");
        JsonNode quote=r.path("indicators").path("quote").get(0);
        JsonNode adjclose=r.path("indicators").path("adjclose");
        JsonNode adj=null;
        if(adjclose.isArray()&&adjclose.size()>0) adj=adjclose.get(0).path("adjclose");
        List<Bar> bars=new ArrayList<>();
        DateTimeFormatter fmt=DateTimeFormatter.ISO_LOCAL_DATE;
        for(int i=0;i<timestamps.size();i++){
            long epoch=timestamps.get(i).asLong();
            String date=Instant.ofEpochSecond(epoch).atZone(ZoneId.of("America/New_York")).toLocalDate().format(fmt);
            double open=quote.path("open").get(i).isNull()?Double.NaN:quote.path("open").get(i).asDouble();
            double high=quote.path("high").get(i).isNull()?Double.NaN:quote.path("high").get(i).asDouble();
            double low=quote.path("low").get(i).isNull()?Double.NaN:quote.path("low").get(i).asDouble();
            double close=quote.path("close").get(i).isNull()?Double.NaN:quote.path("close").get(i).asDouble();
            double adjc= (adj!=null && adj.get(i)!=null && !adj.get(i).isNull())? adj.get(i).asDouble(): close;
            long vol=quote.path("volume").get(i).isNull()?0:quote.path("volume").get(i).asLong();
            if(Double.isNaN(open)&&Double.isNaN(close)) continue;
            bars.add(new Bar(symbol,date,epoch,open,high,low,close,adjc,vol));
        }
        log.info("Fetched {} bars for {}", bars.size(), symbol);
        return bars;
    }
    private String fetchWithRetry(String url) throws Exception{
        int attempts=0;
        long backoff=cfg.retry().backoffMs();
        while(true){
            attempts++;
            try{
                throttle();
                HttpRequest req=HttpRequest.newBuilder(URI.create(url)).timeout(java.time.Duration.ofSeconds(30)).GET().header("User-Agent","Mozilla/5.0").header("Accept","application/json").build();
                HttpResponse<String> resp=http.send(req, HttpResponse.BodyHandlers.ofString());
                if(resp.statusCode()==429||resp.statusCode()>=500) throw new java.io.IOException("HTTP "+resp.statusCode()+" "+resp.body().substring(0,Math.min(200,resp.body().length())));
                if(resp.statusCode()!=200) throw new java.io.IOException("HTTP "+resp.statusCode()+" "+resp.body().substring(0,Math.min(400,resp.body().length())));
                return resp.body();
            }catch(Exception e){
                if(attempts>=cfg.retry().maxAttempts()) throw e;
                log.warn("Fetch failed {}/{} {} -> retry {}ms {}",attempts,cfg.retry().maxAttempts(),url,backoff,e.toString());
                Thread.sleep(backoff+ThreadLocalRandom.current().nextInt(200));
                backoff=Math.min(backoff*2,cfg.retry().maxBackoffMs());
            }
        }
    }
}
