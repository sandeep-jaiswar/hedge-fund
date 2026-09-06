package com.hedgefund.defillama.ingest;
import com.hedgefund.defillama.client.DefillamaClient;
import com.hedgefund.defillama.config.DefillamaConfig;
import com.hedgefund.defillama.store.DefillamaBronzeWriter;
import com.hedgefund.defillama.store.DefillamaSilverTransformer;
import com.hedgefund.datalake.Datalake;
import org.slf4j.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
public class DefillamaIngestService {
    private static final Logger log=LoggerFactory.getLogger(DefillamaIngestService.class);
    private final DefillamaConfig cfg;
    private final DefillamaClient client;
    private final DefillamaBronzeWriter bronze;
    private final DefillamaSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;
    public DefillamaIngestService(DefillamaConfig cfg, Path datalakeRoot){
        this.cfg=cfg;
        this.client=new DefillamaClient(cfg);
        this.bronzeRoot=datalakeRoot.resolve(cfg.paths().bronze());
        this.silverRoot=datalakeRoot.resolve(cfg.paths().silver());
        this.bronze=new DefillamaBronzeWriter(bronzeRoot);
        this.silver=new DefillamaSilverTransformer();
    }
    public void run() throws Exception {
        log.info("defillama ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());
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
        Path out=silver.transform(bronzeRoot, silverRoot, "defillama.csv");
        log.info("Silver wrote {}", out);
        Files.writeString(bronzeRoot.resolve("_watermark.json"), "{\"lastRun\":\""+java.time.Instant.now().toString()+"\",\"keys\":"+cfg.effectiveKeys().size()+"}");
    }
    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return base+"/protocol/"+key;
    }
}
