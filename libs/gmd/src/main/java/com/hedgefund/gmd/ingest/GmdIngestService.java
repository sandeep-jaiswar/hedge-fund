package com.hedgefund.gmd.ingest;
import com.hedgefund.gmd.client.GmdClient;
import com.hedgefund.gmd.config.GmdConfig;
import com.hedgefund.gmd.store.GmdBronzeWriter;
import com.hedgefund.gmd.store.GmdSilverTransformer;
import com.hedgefund.datalake.Datalake;
import org.slf4j.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
public class GmdIngestService {
    private static final Logger log=LoggerFactory.getLogger(GmdIngestService.class);
    private final GmdConfig cfg;
    private final GmdClient client;
    private final GmdBronzeWriter bronze;
    private final GmdSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;
    public GmdIngestService(GmdConfig cfg, Path datalakeRoot){
        this.cfg=cfg;
        this.client=new GmdClient(cfg);
        this.bronzeRoot=datalakeRoot.resolve(cfg.paths().bronze());
        this.silverRoot=datalakeRoot.resolve(cfg.paths().silver());
        this.bronze=new GmdBronzeWriter(bronzeRoot);
        this.silver=new GmdSilverTransformer();
    }
    public void run() throws Exception {
        log.info("gmd ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());
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
        Path out=silver.transform(bronzeRoot, silverRoot, "gmd.csv");
        log.info("Silver wrote {}", out);
        Files.writeString(bronzeRoot.resolve("_watermark.json"), "{\"lastRun\":\""+java.time.Instant.now().toString()+"\",\"keys\":"+cfg.effectiveKeys().size()+"}");
    }
    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return "https://raw.githubusercontent.com/datasets/s-and-p-500/master/data/data.csv";
    }
}
