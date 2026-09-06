package com.hedgefund.sec.ingest;
import com.hedgefund.sec.client.SecClient;
import com.hedgefund.sec.config.SecConfig;
import com.hedgefund.sec.store.SecBronzeWriter;
import com.hedgefund.sec.store.SecSilverTransformer;
import com.hedgefund.datalake.Datalake;
import org.slf4j.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
public class SecIngestService {
    private static final Logger log=LoggerFactory.getLogger(SecIngestService.class);
    private final SecConfig cfg;
    private final SecClient client;
    private final SecBronzeWriter bronze;
    private final SecSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;
    public SecIngestService(SecConfig cfg, Path datalakeRoot){
        this.cfg=cfg;
        this.client=new SecClient(cfg);
        this.bronzeRoot=datalakeRoot.resolve(cfg.paths().bronze());
        this.silverRoot=datalakeRoot.resolve(cfg.paths().silver());
        this.bronze=new SecBronzeWriter(bronzeRoot);
        this.silver=new SecSilverTransformer();
    }
    public void run() throws Exception {
        log.info("sec ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());
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
        Path out=silver.transform(bronzeRoot, silverRoot, "sec.csv");
        log.info("Silver wrote {}", out);
        Files.writeString(bronzeRoot.resolve("_watermark.json"), "{\"lastRun\":\""+java.time.Instant.now().toString()+"\",\"keys\":"+cfg.effectiveKeys().size()+"}");
    }
    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return "https://data.sec.gov/submissions/CIK0000320193.json";
    }
}
