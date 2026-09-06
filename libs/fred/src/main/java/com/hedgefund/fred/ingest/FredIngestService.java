package com.hedgefund.fred.ingest;
import com.hedgefund.fred.client.FredClient;
import com.hedgefund.fred.config.FredConfig;
import com.hedgefund.fred.store.FredBronzeWriter;
import com.hedgefund.fred.store.FredSilverTransformer;
import com.hedgefund.datalake.Datalake;
import org.slf4j.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
public class FredIngestService {
    private static final Logger log=LoggerFactory.getLogger(FredIngestService.class);
    private final FredConfig cfg;
    private final FredClient client;
    private final FredBronzeWriter bronze;
    private final FredSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;
    public FredIngestService(FredConfig cfg, Path datalakeRoot){
        this.cfg=cfg;
        this.client=new FredClient(cfg);
        this.bronzeRoot=datalakeRoot.resolve(cfg.paths().bronze());
        this.silverRoot=datalakeRoot.resolve(cfg.paths().silver());
        this.bronze=new FredBronzeWriter(bronzeRoot);
        this.silver=new FredSilverTransformer();
    }
    public void run() throws Exception {
        log.info("fred ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());
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
        Path out=silver.transform(bronzeRoot, silverRoot, "fred.csv");
        log.info("Silver wrote {}", out);
        Files.writeString(bronzeRoot.resolve("_watermark.json"), "{\"lastRun\":\""+java.time.Instant.now().toString()+"\",\"keys\":"+cfg.effectiveKeys().size()+"}");
    }
    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return "https://raw.githubusercontent.com/datasets/s-and-p-500/master/data/data.csv";
    }
}
