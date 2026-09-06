package com.hedgefund.eia.ingest;
import com.hedgefund.eia.client.EiaClient;
import com.hedgefund.eia.config.EiaConfig;
import com.hedgefund.eia.store.EiaBronzeWriter;
import com.hedgefund.eia.store.EiaSilverTransformer;
import com.hedgefund.datalake.Datalake;
import org.slf4j.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
public class EiaIngestService {
    private static final Logger log=LoggerFactory.getLogger(EiaIngestService.class);
    private final EiaConfig cfg;
    private final EiaClient client;
    private final EiaBronzeWriter bronze;
    private final EiaSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;
    public EiaIngestService(EiaConfig cfg, Path datalakeRoot){
        this.cfg=cfg;
        this.client=new EiaClient(cfg);
        this.bronzeRoot=datalakeRoot.resolve(cfg.paths().bronze());
        this.silverRoot=datalakeRoot.resolve(cfg.paths().silver());
        this.bronze=new EiaBronzeWriter(bronzeRoot);
        this.silver=new EiaSilverTransformer();
    }
    public void run() throws Exception {
        log.info("eia ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());
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
        Path out=silver.transform(bronzeRoot, silverRoot, "eia.csv");
        log.info("Silver wrote {}", out);
        Files.writeString(bronzeRoot.resolve("_watermark.json"), "{\"lastRun\":\""+java.time.Instant.now().toString()+"\",\"keys\":"+cfg.effectiveKeys().size()+"}");
    }
    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return "https://www.eia.gov/dnav/pet/hist_xls/RBRTEd.xls";
    }
}
