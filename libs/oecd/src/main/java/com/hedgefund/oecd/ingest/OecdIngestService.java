package com.hedgefund.oecd.ingest;
import com.hedgefund.oecd.client.OecdClient;
import com.hedgefund.oecd.config.OecdConfig;
import com.hedgefund.oecd.store.OecdBronzeWriter;
import com.hedgefund.oecd.store.OecdSilverTransformer;
import com.hedgefund.datalake.Datalake;
import org.slf4j.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
public class OecdIngestService {
    private static final Logger log=LoggerFactory.getLogger(OecdIngestService.class);
    private final OecdConfig cfg;
    private final OecdClient client;
    private final OecdBronzeWriter bronze;
    private final OecdSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;
    public OecdIngestService(OecdConfig cfg, Path datalakeRoot){
        this.cfg=cfg;
        this.client=new OecdClient(cfg);
        this.bronzeRoot=datalakeRoot.resolve(cfg.paths().bronze());
        this.silverRoot=datalakeRoot.resolve(cfg.paths().silver());
        this.bronze=new OecdBronzeWriter(bronzeRoot);
        this.silver=new OecdSilverTransformer();
    }
    public void run() throws Exception {
        log.info("oecd ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());
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
        Path out=silver.transform(bronzeRoot, silverRoot, "oecd.csv");
        log.info("Silver wrote {}", out);
        Files.writeString(bronzeRoot.resolve("_watermark.json"), "{\"lastRun\":\""+java.time.Instant.now().toString()+"\",\"keys\":"+cfg.effectiveKeys().size()+"}");
    }
    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return "https://sdmx.oecd.org/public/rest/data/OECD.SDD.STES,DSD_KEI@DF_KEI,4.0/USA.CP";
    }
}
