package com.hedgefund.cboe.ingest;
import com.hedgefund.cboe.client.CboeClient;
import com.hedgefund.cboe.config.CboeConfig;
import com.hedgefund.cboe.store.CboeBronzeWriter;
import com.hedgefund.cboe.store.CboeSilverTransformer;
import com.hedgefund.datalake.Datalake;
import org.slf4j.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
public class CboeIngestService {
    private static final Logger log=LoggerFactory.getLogger(CboeIngestService.class);
    private final CboeConfig cfg;
    private final CboeClient client;
    private final CboeBronzeWriter bronze;
    private final CboeSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;
    public CboeIngestService(CboeConfig cfg, Path datalakeRoot){
        this.cfg=cfg;
        this.client=new CboeClient(cfg);
        this.bronzeRoot=datalakeRoot.resolve(cfg.paths().bronze());
        this.silverRoot=datalakeRoot.resolve(cfg.paths().silver());
        this.bronze=new CboeBronzeWriter(bronzeRoot);
        this.silver=new CboeSilverTransformer();
    }
    public void run() throws Exception {
        log.info("cboe ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());
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
                    String raw=client.fetchRaw(url);
                    bronze.write(key, raw);
                    log.info("Done {} len={}", key, raw.length());
                }catch(Exception e){ log.error("Failed {}", key, e); throw new RuntimeException(e); }
                finally{ sem.release(); }
            }));
        }
        for(Future<?> f: futures) f.get(60, TimeUnit.SECONDS);
        }
        Path out=silver.transform(bronzeRoot, silverRoot, "cboe.csv");
        log.info("Silver wrote {}", out);
        Files.writeString(bronzeRoot.resolve("_watermark.json"), "{\"lastRun\":\""+java.time.Instant.now().toString()+"\",\"keys\":"+cfg.effectiveKeys().size()+"}");
    }
    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return "https://query1.finance.yahoo.com/v8/finance/chart/%5EVIX?interval=1d&range=1mo";
    }
}
