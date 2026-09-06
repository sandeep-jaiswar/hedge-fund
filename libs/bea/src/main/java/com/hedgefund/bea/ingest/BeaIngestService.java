package com.hedgefund.bea.ingest;
import com.hedgefund.bea.client.BeaClient;
import com.hedgefund.bea.config.BeaConfig;
import com.hedgefund.bea.store.BeaBronzeWriter;
import com.hedgefund.bea.store.BeaSilverTransformer;
import com.hedgefund.datalake.Datalake;
import org.slf4j.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
public class BeaIngestService {
    private static final Logger log=LoggerFactory.getLogger(BeaIngestService.class);
    private final BeaConfig cfg;
    private final BeaClient client;
    private final BeaBronzeWriter bronze;
    private final BeaSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;
    public BeaIngestService(BeaConfig cfg, Path datalakeRoot){
        this.cfg=cfg;
        this.client=new BeaClient(cfg);
        this.bronzeRoot=datalakeRoot.resolve(cfg.paths().bronze());
        this.silverRoot=datalakeRoot.resolve(cfg.paths().silver());
        this.bronze=new BeaBronzeWriter(bronzeRoot);
        this.silver=new BeaSilverTransformer();
    }
    public void run() throws Exception {
        log.info("bea ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());
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
        Path out=silver.transform(bronzeRoot, silverRoot, "bea.csv");
        log.info("Silver wrote {}", out);
        Files.writeString(bronzeRoot.resolve("_watermark.json"), "{\"lastRun\":\""+java.time.Instant.now().toString()+"\",\"keys\":"+cfg.effectiveKeys().size()+"}");
    }
    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return "https://apps.bea.gov/api/data?UserID=demo&method=GetData&DataSetName=NIPA&TableName=T10101&Frequency=Q&Year=2023";
    }
}
