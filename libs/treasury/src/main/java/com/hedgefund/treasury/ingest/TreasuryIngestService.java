package com.hedgefund.treasury.ingest;
import com.hedgefund.treasury.client.TreasuryClient;
import com.hedgefund.treasury.config.TreasuryConfig;
import com.hedgefund.treasury.store.TreasuryBronzeWriter;
import com.hedgefund.treasury.store.TreasurySilverTransformer;
import com.hedgefund.datalake.Datalake;
import org.slf4j.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
public class TreasuryIngestService {
    private static final Logger log=LoggerFactory.getLogger(TreasuryIngestService.class);
    private final TreasuryConfig cfg;
    private final TreasuryClient client;
    private final TreasuryBronzeWriter bronze;
    private final TreasurySilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;
    public TreasuryIngestService(TreasuryConfig cfg, Path datalakeRoot){
        this.cfg=cfg;
        this.client=new TreasuryClient(cfg);
        this.bronzeRoot=datalakeRoot.resolve(cfg.paths().bronze());
        this.silverRoot=datalakeRoot.resolve(cfg.paths().silver());
        this.bronze=new TreasuryBronzeWriter(bronzeRoot);
        this.silver=new TreasurySilverTransformer();
    }
    public void run() throws Exception {
        log.info("treasury ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());
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
        Path out=silver.transform(bronzeRoot, silverRoot, "treasury.csv");
        log.info("Silver wrote {}", out);
        Files.writeString(bronzeRoot.resolve("_watermark.json"), "{\"lastRun\":\""+java.time.Instant.now().toString()+"\",\"keys\":"+cfg.effectiveKeys().size()+"}");
    }
    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return "https://home.treasury.gov/resource-center/data-chart-center/interest-rates/daily-treasury-rates.csv/2024/all?type=daily_treasury_yield_curve";
    }
}
