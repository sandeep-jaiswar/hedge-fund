package com.hedgefund.baostock.ingest;
import com.hedgefund.baostock.client.BaostockClient;
import com.hedgefund.baostock.config.BaostockConfig;
import com.hedgefund.baostock.store.BaostockBronzeWriter;
import com.hedgefund.baostock.store.BaostockSilverTransformer;
import com.hedgefund.observability.logging.CorrelationId;
import org.slf4j.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
public class BaostockIngestService {
    private static final Logger log=LoggerFactory.getLogger(BaostockIngestService.class);
    private final BaostockConfig cfg;
    private final BaostockClient client;
    private final BaostockBronzeWriter bronze;
    private final BaostockSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;
    public BaostockIngestService(BaostockConfig cfg, Path datalakeRoot){
        this.cfg=cfg;
        this.client=new BaostockClient(cfg);
        this.bronzeRoot=datalakeRoot.resolve(cfg.ingestConfig().paths().bronze());
        this.silverRoot=datalakeRoot.resolve(cfg.ingestConfig().paths().silver());
        this.bronze=new BaostockBronzeWriter(bronzeRoot);
        this.silver=new BaostockSilverTransformer();
    }
    public void run() throws Exception {
        CorrelationId.withContext("baostock");
        try {
            log.info("baostock ingest start keys={} base={}", cfg.symbols(), cfg.baseUrl());
            Files.createDirectories(bronzeRoot);
            Files.createDirectories(silverRoot);
            try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            Semaphore sem=new Semaphore(cfg.ingestConfig().concurrency());
            List<Future<?>> futures=new ArrayList<>();
            for(String key: cfg.symbols()){
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
            log.info("baostock ingest done.");
        } finally {
            CorrelationId.clear();
        }
    }
    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return base+"/api/query/history_k_data_json?code="+key+"&fields=date,code,open,high,low,close,volume&start=2016-01-01&end=2026-12-31";
    }
}
