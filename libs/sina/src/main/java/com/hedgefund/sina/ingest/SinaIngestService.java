package com.hedgefund.sina.ingest;

import com.hedgefund.sina.client.SinaClient;
import com.hedgefund.sina.config.SinaConfig;
import com.hedgefund.sina.store.SinaBronzeWriter;
import com.hedgefund.sina.store.SinaSilverTransformer;
import com.hedgefund.observability.logging.CorrelationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class SinaIngestService {

    private static final Logger log = LoggerFactory.getLogger(SinaIngestService.class);

    private final SinaConfig cfg;
    private final SinaClient client;
    private final SinaBronzeWriter bronze;
    private final SinaSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;

    public SinaIngestService(SinaConfig cfg, Path datalakeRoot) {
        this.cfg = cfg;
        this.client = new SinaClient(cfg);
        this.bronzeRoot = datalakeRoot.resolve(cfg.ingestConfig().paths().bronze());
        this.silverRoot = datalakeRoot.resolve(cfg.ingestConfig().paths().silver());
        this.bronze = new SinaBronzeWriter(bronzeRoot);
        this.silver = new SinaSilverTransformer();
    }

    public void run() throws Exception {
        CorrelationId.withContext("sina");
        log.info("sina ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());

        Files.createDirectories(bronzeRoot);
        Files.createDirectories(silverRoot);

        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            Semaphore sem = new Semaphore(cfg.ingestConfig().concurrency());
            List<Future<?>> futures = new ArrayList<>();

            for (String key : cfg.effectiveKeys()) {
                sem.acquire();
                futures.add(exec.submit(() -> {
                    CorrelationId.withContext("sina");
                    try {
                        String url = buildUrl(key);
                        String raw = client.fetchRaw(url);
                        bronze.write(key, raw);
                        log.info("Done {} len={}", key, raw.length());
                    } catch (Exception e) {
                        log.error("Failed {}", key, e);
                        throw new RuntimeException(e);
                    } finally {
                        sem.release();
                        CorrelationId.clear();
                    }
                }));
            }

            for (Future<?> f : futures) {
                f.get(60, TimeUnit.SECONDS);
            }
        }

        Path out = silver.transform(bronzeRoot, silverRoot, "sina.csv");
        log.info("Silver wrote {}", out);

        Files.writeString(bronzeRoot.resolve("_watermark.json"),
            "{\"lastRun\":\"" + java.time.Instant.now() + "\",\"keys\":" + cfg.effectiveKeys().size() + "}");

        CorrelationId.clear();
    }

    private String buildUrl(String key) {
        String base = cfg.baseUrl();
        return "https://hq.sinajs.cn/list=sh600000";
    }
}
