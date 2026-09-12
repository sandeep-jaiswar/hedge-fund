package com.hedgefund.tencent.ingest;
import com.hedgefund.tencent.client.TencentClient;
import com.hedgefund.tencent.config.TencentConfig;
import com.hedgefund.tencent.store.TencentBronzeWriter;
import com.hedgefund.tencent.store.TencentSilverTransformer;
import com.hedgefund.observability.logging.CorrelationId;
import org.slf4j.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
public class TencentIngestService {
    private static final Logger log=LoggerFactory.getLogger(TencentIngestService.class);
    private final TencentConfig cfg;
    private final TencentClient client;
    private final TencentBronzeWriter bronze;
    private final TencentSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;
    public TencentIngestService(TencentConfig cfg, Path datalakeRoot){
        this.cfg=cfg;
        this.client=new TencentClient(cfg);
        this.bronzeRoot=datalakeRoot.resolve(cfg.ingestConfig().paths().bronze());
        this.silverRoot=datalakeRoot.resolve(cfg.ingestConfig().paths().silver());
        this.bronze=new TencentBronzeWriter(bronzeRoot);
        this.silver=new TencentSilverTransformer();
    }
    public void run() throws Exception {
        CorrelationId.withContext("tencent");
        try {
            log.info("tencent ingest start keys={} base={}", cfg.symbols(), cfg.baseUrl());
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
            log.info("tencent ingest done.");
        } finally {
            CorrelationId.clear();
        }
    }
    private String buildUrl(String key){
        String base=cfg.baseUrl();
        return "https://qt.gtimg.cn/q=sh600000";
    }
}
