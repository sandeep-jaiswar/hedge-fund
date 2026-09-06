package com.hedgefund.investing.ingest;
import com.hedgefund.investing.client.InvestingClient;
import com.hedgefund.investing.config.InvestingConfig;
import com.hedgefund.investing.store.InvestingBronzeWriter;
import com.hedgefund.investing.store.InvestingSilverTransformer;
import com.hedgefund.datalake.Datalake;
import org.slf4j.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
public class InvestingIngestService {
    private static final Logger log=LoggerFactory.getLogger(InvestingIngestService.class);
    private final InvestingConfig cfg;
    private final InvestingClient client;
    private final InvestingBronzeWriter bronze;
    private final InvestingSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;
    public InvestingIngestService(InvestingConfig cfg, Path datalakeRoot){
        this.cfg=cfg;
        this.client=new InvestingClient(cfg);
        this.bronzeRoot=datalakeRoot.resolve(cfg.paths().bronze());
        this.silverRoot=datalakeRoot.resolve(cfg.paths().silver());
        this.bronze=new InvestingBronzeWriter(bronzeRoot);
        this.silver=new InvestingSilverTransformer();
    }
    public void run() throws Exception {
        log.info("investing ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());
        Files.createDirectories(bronzeRoot);
        Files.createDirectories(silverRoot);
        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
        Semaphore sem=new Semaphore(cfg.concurrency());
        List<Future<?>> futures=new ArrayList<>();
        for(String key: cfg.effectiveKeys()){
            sem.acquire();
            futures.add(exec.submit(()->{
                try{
                    String url=buildUrl(key);
                    String raw = client.fetchRaw(url);
                    bronze.write(key, raw);
                    log.info("Done {} len={}", key, raw.length());
                }catch(Exception e){ log.error("Failed {}", key, e); throw new RuntimeException(e); }
                finally{ sem.release(); }
            }));
        }
        for(Future<?> f: futures) f.get(60, TimeUnit.SECONDS);
        }
        Path out=silver.transform(bronzeRoot, silverRoot, "investing.csv");
        log.info("Silver wrote {}", out);
        Files.writeString(bronzeRoot.resolve("_watermark.json"), "{\"lastRun\":\""+java.time.Instant.now().toString()+"\",\"keys\":"+cfg.effectiveKeys().size()+"}");
    }
    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return "https://query1.finance.yahoo.com/v8/finance/chart/SPY?interval=1d&range=1mo";
    }
}
