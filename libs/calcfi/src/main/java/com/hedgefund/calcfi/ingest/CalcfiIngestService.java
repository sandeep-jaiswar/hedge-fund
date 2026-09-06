package com.hedgefund.calcfi.ingest;
import com.hedgefund.calcfi.client.CalcfiClient;
import com.hedgefund.calcfi.config.CalcfiConfig;
import com.hedgefund.calcfi.store.CalcfiBronzeWriter;
import com.hedgefund.calcfi.store.CalcfiSilverTransformer;
import com.hedgefund.datalake.Datalake;
import org.slf4j.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
public class CalcfiIngestService {
    private static final Logger log=LoggerFactory.getLogger(CalcfiIngestService.class);
    private final CalcfiConfig cfg;
    private final CalcfiClient client;
    private final CalcfiBronzeWriter bronze;
    private final CalcfiSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;
    public CalcfiIngestService(CalcfiConfig cfg, Path datalakeRoot){
        this.cfg=cfg;
        this.client=new CalcfiClient(cfg);
        this.bronzeRoot=datalakeRoot.resolve(cfg.paths().bronze());
        this.silverRoot=datalakeRoot.resolve(cfg.paths().silver());
        this.bronze=new CalcfiBronzeWriter(bronzeRoot);
        this.silver=new CalcfiSilverTransformer();
    }
    public void run() throws Exception {
        log.info("calcfi ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());
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
        Path out=silver.transform(bronzeRoot, silverRoot, "calcfi.csv");
        log.info("Silver wrote {}", out);
        Files.writeString(bronzeRoot.resolve("_watermark.json"), "{\"lastRun\":\""+java.time.Instant.now().toString()+"\",\"keys\":"+cfg.effectiveKeys().size()+"}");
    }
    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return "https://raw.githubusercontent.com/datasets/s-and-p-500/master/data/data.csv";
    }
}
