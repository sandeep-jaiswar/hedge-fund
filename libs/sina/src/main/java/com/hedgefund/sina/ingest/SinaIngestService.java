package com.hedgefund.sina.ingest;
import com.hedgefund.sina.client.SinaClient;
import com.hedgefund.sina.config.SinaConfig;
import com.hedgefund.sina.store.SinaBronzeWriter;
import com.hedgefund.sina.store.SinaSilverTransformer;
import com.hedgefund.datalake.Datalake;
import org.slf4j.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
public class SinaIngestService {
    private static final Logger log=LoggerFactory.getLogger(SinaIngestService.class);
    private final SinaConfig cfg;
    private final SinaClient client;
    private final SinaBronzeWriter bronze;
    private final SinaSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;
    public SinaIngestService(SinaConfig cfg, Path datalakeRoot){
        this.cfg=cfg;
        this.client=new SinaClient(cfg);
        this.bronzeRoot=datalakeRoot.resolve(cfg.paths().bronze());
        this.silverRoot=datalakeRoot.resolve(cfg.paths().silver());
        this.bronze=new SinaBronzeWriter(bronzeRoot);
        this.silver=new SinaSilverTransformer();
    }
    public void run() throws Exception {
        log.info("sina ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());
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
        Path out=silver.transform(bronzeRoot, silverRoot, "sina.csv");
        log.info("Silver wrote {}", out);
        Files.writeString(bronzeRoot.resolve("_watermark.json"), "{\"lastRun\":\""+java.time.Instant.now().toString()+"\",\"keys\":"+cfg.effectiveKeys().size()+"}");
    }
    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return "https://hq.sinajs.cn/list=sh600000";
    }
}
