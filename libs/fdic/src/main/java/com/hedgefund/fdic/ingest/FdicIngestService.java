package com.hedgefund.fdic.ingest;

import com.hedgefund.fdic.client.FdicClient;
import com.hedgefund.fdic.config.FdicConfig;
import com.hedgefund.fdic.store.FdicBronzeWriter;
import com.hedgefund.fdic.store.FdicSilverTransformer;
import com.hedgefund.observability.logging.CorrelationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class FdicIngestService {
    private static final Logger log = LoggerFactory.getLogger(FdicIngestService.class);
    private final FdicConfig cfg;
    private final FdicClient client;
    private final FdicBronzeWriter bronze;
    private final FdicSilverTransformer silver;
    private final Path bronzeRoot;
    private final Path silverRoot;

    public FdicIngestService(FdicConfig cfg, Path datalakeRoot) {
        this.cfg = cfg;
        this.client = new FdicClient(cfg);
        this.bronzeRoot = datalakeRoot.resolve(cfg.ingestConfig().paths().bronze());
        this.silverRoot = datalakeRoot.resolve(cfg.ingestConfig().paths().silver());
        this.bronze = new FdicBronzeWriter(bronzeRoot);
        this.silver = new FdicSilverTransformer();
    }

    public void run() throws Exception {
        CorrelationId.withContext("fdic");
        try {
            log.info("fdic ingest start keys={} base={}", cfg.effectiveKeys(), cfg.baseUrl());
            Files.createDirectories(bronzeRoot);
            Files.createDirectories(silverRoot);
            try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
                Semaphore sem = new Semaphore(cfg.ingestConfig().concurrency());
                List<Future<?>> futures = new ArrayList<>();
                for (String key : cfg.effectiveKeys()) {
                    sem.acquire();
                    futures.add(exec.submit(() -> {
                        try {
                            CorrelationId.withContext("fdic");
                            String url = buildUrl(key);
                            String raw = client.fetchRaw(url);
                            bronze.write(key, raw);
                            log.info("Done {} len={}", key, raw.length());
                        } catch (Exception e) {
                            log.error("Failed {}", key, e);
                            throw new RuntimeException(e);
                        } finally {
                            CorrelationId.clear();
                            sem.release();
                        }
                    }));
                }
                for (Future<?> f : futures) f.get(60, TimeUnit.SECONDS);
            }
            Path out = silver.transform(bronzeRoot, silverRoot, "fdic.csv");
            log.info("Silver wrote {}", out);
            Files.writeString(bronzeRoot.resolve("_watermark.json"), "{\"lastRun\":\"" + java.time.Instant.now().toString() + "\",\"keys\":" + cfg.effectiveKeys().size() + "}");
        } finally {
            CorrelationId.clear();
        }
    }

    private String buildUrl(String key) {
        String base = cfg.baseUrl();
        return "https://raw.githubusercontent.com/datasets/s-and-p-500/master/data/data.csv";
    }
}
